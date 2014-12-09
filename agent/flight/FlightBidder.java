package agent.flight;

import se.sics.tac.aw.BidString;
import se.sics.tac.aw.Quote;
import se.sics.tac.aw.Transaction;
import agent.*;
import agent.logging.AgentLogger;

import java.util.*;
import java.util.logging.Logger;

public class FlightBidder implements Auction.Watcher {
    
    // How confident that we'll get our ticket?
    // Low confidence -> Bid lower, less likely to get ticket
    // High confidence -> Bid higher, more likely to get ticket
    private static final double BID_CONFIDENCE = 0.95d;

    private final AgentLogger logger;

    private final FlightAgent flightAgent;
    private final FlightAuction auction;
    private final FlightPriceMonitor monitor;
    private final List<agent.Package> packages = new ArrayList<agent.Package>();
    private boolean bidDirtyFlag = false;
    private boolean gameStarted = false;

    private int lastBidQuantity = 0;
    private float lastBidPrice = 0f;

    public FlightBidder(FlightAgent flightAgent, FlightAuction auction, AgentLogger logger) {
        this.flightAgent = flightAgent;
        this.auction = auction;
        this.logger = logger.getSublogger(auction.getDay() + "." + auction.getArrival());
        this.monitor = new FlightPriceMonitor(auction);
        // Register to watch this auction
        this.auction.addWatcher(this);
    }

    public void gameStopped() {
        logger.log("Stopping bidder");
        // stop receiving updates on auction
        auction.removeWatcher(this);
        gameStarted = false;
    }

    public void addPackage(agent.Package pack) {
        logger.log("Adding package");
        packages.add(pack);
    }

    public void start() {
        gameStarted = true;
        refreshBid();
    }

    public void stop() {
        logger.log("Clearing packages");
        packages.clear();
        refreshBid();
        gameStarted = false;
    }

    public float estimatedPrice() {
        // Get predicted minimum from monitor
        return (float)monitor.predictMinimumPrice(getTimeStep());
    }

    public void auctionQuoteUpdated(Auction<?> auction, Quote quote) {
        // Update price monitor with new prices information
        // TODO: Make sure this only happens every 10 seconds when the price perturbs!
        logger.log("Quote updated at " + getTimeStep() + ": " + quote.getAskPrice());
        monitor.addQuote((double)quote.getAskPrice(), getTimeStep());

        // only refresh bid at 20 second intervals to avoid duplicate bid problems
        if (getTimeStep() % 2 == 0) {
            refreshBid();
        }
    }

    public void auctionBidUpdated(Auction<?> auction, BidString bidString) {
        // If this is called when a bid is accepted,
        // then this checks if we have a waiting bid to submit
        logger.log("Bid updated");
        if (bidDirtyFlag) {
            refreshBid();
        }
    }

    public void auctionBidRejected(Auction<?> auction, BidString bidString) {
        logger.log("Bid rejected", AgentLogger.WARNING);
    }

    public void auctionBidError(Auction<?> auction, BidString bidString, int error) {
        logger.log("Bid error " + error, AgentLogger.WARNING);
    }

    public void auctionBuySuccessful(Auction<?> auction, List<Buyable> tickets) {
        // We got some flight tickets!
        for (Buyable b : tickets) {
            FlightTicket ticket = (FlightTicket)b;
            flightAgent.addTicket(ticket);

            // Fulfill packages one-by-one.
            // There should always be exactly the same number of tickets
            // as packages.
            agent.Package pack = packages.remove(0);
            if (this.auction.getArrival()) {
                pack.setArrivalTicket(ticket);
            } else {
                pack.setDepartureTicket(ticket);
            }
        }
        refreshBid();
    }

    @Override
    public void auctionSellSuccessful(Auction<?> auction, int numSold) { }

    public void auctionClosed(Auction<?> auction) {
        logger.log("Auction closed");
    }

    private int getTimeStep() {
        return (int)(flightAgent.agent.getTime() / 10000);
    }

    private void refreshBid() {
        // Don't bid if game has stopped
        if (!gameStarted) return;

        int quantity = packages.size();
        float price = calcPrice();

        // Don't bid if current bid is still valid and quantity hasn't changed
        // This is security for accidentally buying multiple copies
        if (quantity == lastBidQuantity && 
            lastBidPrice > auction.getAskPrice()) {
            return;
        }

        bidDirtyFlag = true;
        
        try {
            auction.wipeBid();
            if (quantity > 0) {
                auction.modifyBidPoint(quantity, price);
            }

            logger.log("Submitting bid (" + packages.size() + ", " + price + ")");
            auction.submitBid();
        } catch (BidInUseException e) {
            // return early, don't unset bidDirtyFlag
            return;
        }

        lastBidQuantity = quantity;
        lastBidPrice = price;

        bidDirtyFlag = false;
    }

    private float calcPrice() {
        // Get price probability distribution
        // We can't just select the estimated minimum price because
        // the odds are only 50/50 the price will go below that.
        // Instead, select a price with a good probability of being reached.
        List<Double> dist = monitor.priceCumulativeDist(getTimeStep());

        double confidence = monitor.getConfidence();
        logger.log("Confidence: " + confidence);
        logger.log("Min: " + monitor.predictMinimumPrice(getTimeStep()));
        int min = (int)FlightAgent.PRICE_MIN, max = (int)FlightAgent.PRICE_MAX;

        // Don't bid if no ask price yet
        if (auction.getAskPrice() < min) {
            return (float)min;
        }

        // In the final few rounds, we should just pay the ask price
        if (getTimeStep() >= FlightAgent.MAX_TIME - 2) {
            // A dollar for luck!
            float price = auction.getAskPrice() + 1f;
            logger.log("PANIC MODE: " + price);
            return price;
        }

        // Find price with highest expected return
        int bestPrice = (int)FlightAgent.PRICE_MIN;
        double bestExpectedOutcome = Double.MAX_VALUE;
        for (int price = min; price < max; price ++) {
            // Probabiliy price will be below this, assuming confidence in estimate
            double probWithConf = dist.get(price);
            // Probability price will be below this if estimate is incorrect
            // TODO: Make this more accurate
            double probWoutConf = ((double)price - FlightAgent.PRICE_MIN) /
                ((double)auction.getAskPrice() - FlightAgent.PRICE_MIN);
            if (probWoutConf > 1d) probWoutConf = 1d;

            // Potential bad price if estimate is incorrect
            double highPrice = FlightAgent.PRICE_MAX;
            // Potential good price if estimate is incorrect
            double lowPrice = (double)auction.getAskPrice();

            // Outcome if estimate is correct and bid is above true minimum
            double correctOutcomeHigh = confidence * probWithConf * (double)price;
            // Outcome if estimate is correct and bid is below true minimum
            double correctOutcomeLow = confidence * (1d - probWithConf) * highPrice;

            // Outcome if estimate is incorrect and bid is above true minimum
            double incorrectOutcomeHigh = (1d - confidence) * probWoutConf * (double)price;
            // Outcome if estimate is incorrect and bid is below true minimum
            double incorrectOutcomeLow = (1d - confidence) * (1d - probWoutConf) * lowPrice;

            // Expected outcome
            double expectedOutcome = correctOutcomeHigh + correctOutcomeLow +
                incorrectOutcomeHigh + incorrectOutcomeLow;
            
            if (expectedOutcome < bestExpectedOutcome) {
                bestPrice = price;
                bestExpectedOutcome = expectedOutcome;
            }
        }

        return bestPrice;
    }
}
