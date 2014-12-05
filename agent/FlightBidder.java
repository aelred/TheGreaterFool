package agent;

import se.sics.tac.aw.BidString;
import se.sics.tac.aw.Quote;
import se.sics.tac.aw.Transaction;

import java.util.*;
import java.util.logging.Logger;

public class FlightBidder implements Auction.Watcher {
    
    // How confident that we'll get our ticket?
    // Low confidence -> Bid lower, less likely to get ticket
    // High confidence -> Bid higher, more likely to get ticket
    private static final double BID_CONFIDENCE = 0.95d;

    private final Logger log;

    private final FlightAgent flightAgent;
    private final FlightAuction auction;
    private final FlightPriceMonitor monitor;
    private final List<Package> packages = new ArrayList<Package>();
    private boolean bidDirtyFlag = false;
    private boolean gameStarted = true;

    private int lastBidQuantity = 0;
    private float lastBidPrice = 0f;

    public FlightBidder(FlightAgent flightAgent, FlightAuction auction) {
        this.flightAgent = flightAgent;
        this.auction = auction;
        this.log = Logger.getLogger(FlightAgent.log.getName() + 
            "." + auction.getDay() + "." + auction.getArrival());
        this.monitor = new FlightPriceMonitor(auction);
        // Register to watch this auction
        this.auction.addWatcher(this);
    }

    public void gameStopped() {
        log.info("Stopping bidder");
        // stop receiving updates on auction
        auction.removeWatcher(this);
        gameStarted = false;
    }

    public void addPackage(Package pack) {
        log.info("Adding package");
        packages.add(pack);
        refreshBid();
    }

    public void clearPackages() {
        log.info("Clearing packages");
        packages.clear();
        refreshBid();
    }

    public void auctionQuoteUpdated(Auction<?> auction, Quote quote) {
        // Update price monitor with new prices information
        // TODO: Make sure this only happens every 10 seconds when the price perturbs!
        log.info("Quote updated: " + quote.getAskPrice());
        monitor.addQuote((double)quote.getAskPrice(), getTimeStep());

        // only refresh bid at 20 second intervals to avoid duplicate bid problems
        if (getTimeStep() % 2 == 0) {
            refreshBid();
        }
    }

    public void auctionBidUpdated(Auction<?> auction, BidString bidString) {
        // If this is called when a bid is accepted,
        // then this checks if we have a waiting bid to submit
        log.fine("Bid updated");
        if (bidDirtyFlag) {
            refreshBid();
        }
    }

    public void auctionBidRejected(Auction<?> auction, BidString bidString) {
        log.warning("Bid rejected");
    }

    public void auctionBidError(Auction<?> auction, BidString bidString, int error) {
        log.warning("Bid error " + error);
    }

    public void auctionTransaction(Auction<?> auction, List<Buyable> tickets) {
        // We got some flight tickets!
        for (Buyable b : tickets) {
            FlightTicket ticket = (FlightTicket)b;
            flightAgent.addTicket(ticket);

            // Fulfill packages one-by-one.
            // There should always be exactly the same number of tickets
            // as packages.
            Package pack = packages.remove(0);
            if (this.auction.getArrival()) {
                pack.setArrivalTicket(ticket);
            } else {
                pack.setDepartureTicket(ticket);
            }
        }
        refreshBid();
    }

    public void auctionClosed(Auction<?> auction) {
        log.warning("Auction closed");
    }

    private int getTimeStep() {
        return (int)(flightAgent.agent.getTime() / 10000);
    }

    private void refreshBid() {
        // Don't bid if game has stopped
        if (!gameStarted) {
            throw new IllegalStateException("Cannot bid on stopped game.");
        }

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

            log.info(
                "Submitting bid (" + packages.size() + ", " + price + ")");
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
        log.info("Confidence: " + confidence);
        log.info("Min: " + monitor.predictMinimumPrice(getTimeStep()));
        int min = (int)FlightAgent.PRICE_MIN, max = (int)FlightAgent.PRICE_MAX;

        // In the final few rounds, we should just pay the ask price
        if (getTimeStep() >= FlightAgent.MAX_TIME - 2) {
            // A dollar for luck!
            float price = auction.getAskPrice() + 1f;
            log.info("PANIC MODE: " + price);
            return price;
        }

        // Find price with highest expected return
        int bestPrice = (int)FlightAgent.PRICE_MIN;
        double bestExpectedOutcome = Double.MAX_VALUE;
        for (int price = min; price < max; price ++) {
            // Probabiliy price will be below this, assuming confidence in estimate
            double probWithConf = dist.get(price);
            // Probability price will be below this if estimate is incorrect
            // Very approximate, assume minimum price is uniformly below current price
            // TODO: Make this more accurate
            double probWoutConf = ((double)dist.get(price) - (double)min) / 
                (double)(auction.getAskPrice() - min);
            if (probWoutConf > 1d) {
                probWoutConf = 1d;
            }

            // Potential bad price if estimate is incorrect
            int highPrice = max;
            // Potential good price if estimate is incorrect
            int lowPrice = min;

            // Outcome if estimate is correct and bid is above true minimum
            double correctOutcomeHigh = confidence * probWithConf * (double)price;
            // Outcome if estimate is correct and bid is below true minimum
            double correctOutcomeLow = confidence * (1d - probWoutConf) * (double)max;

            // Outcome if estimate is incorrect and bid is too high
            double incorrectOutcomeHigh = 
                (1d - confidence) * probWoutConf * (double)price;
            // Outcome if estimate is incorrect and bid is too low
            double incorrectOutcomeLow = 
                (1d - confidence) * (1d - probWoutConf) * (double)lowPrice;

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
