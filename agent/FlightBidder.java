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
        refreshBid();
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
        if (getTimeStep() >= FlightAgent.MAX_TIME - 1) {
            // A dollar for luck!
            float price = auction.getAskPrice() + 1f;
            log.info("PANIC MODE: " + price);
            return price;
        }

        // Find price with highest expected return
        int bestPrice = (int)FlightAgent.PRICE_MIN;
        double bestExpectedCost = Double.MAX_VALUE;
        for (int price = min; price < max; price ++) {
            double prob = dist.get(price);

            // Calculate potential loss if price estimate is incorrect
            double incorrectLoss = (double)price * (1d - confidence);
            // Calculate loss if estimate is correct and we buy at this price
            double correctLoss = (double)price * confidence * prob;

            // A potentially bad price: the maximum possible
            double badPrice = (double)max;
            // Calculate loss if estimate is too low and ticket is bought
            // later at a higher price.
            double tooLowLoss = badPrice * confidence * (1d - prob);
            // Calculate expected cost
            double expectedCost = incorrectLoss + correctLoss + tooLowLoss;

            if (expectedCost < bestExpectedCost) {
                bestPrice = price;
                bestExpectedCost = expectedCost;
            }
        }

        return bestPrice;
    }
}
