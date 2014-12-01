package agent;

import se.sics.tac.aw.BidString;
import se.sics.tac.aw.Quote;
import se.sics.tac.aw.Transaction;

import java.util.*;

public class FlightBidder implements Auction.Watcher {
    
    // How confident that we'll get our ticket?
    // Low confidence -> Bid lower, less likely to get ticket
    // High confidence -> Bid higher, more likely to get ticket
    private static final double BID_CONFIDENCE = 0.95d;

    private final FlightAgent flightAgent;
    private final FlightAuction auction;
    private final FlightPriceMonitor monitor;
    private int numWanted = 0;
    private boolean bidDirtyFlag = false;

    public FlightBidder(FlightAgent flightAgent, FlightAuction auction) {
        this.flightAgent = flightAgent;
        this.auction = auction;
        this.monitor = new FlightPriceMonitor(auction);
        // Register to watch this auction
        this.auction.addWatcher(this);
    }

    public void addWanted() {
        numWanted++;
        refreshBid();
    }

    public void removeWanted() {
        numWanted--;
        refreshBid();
    }

    public void auctionQuoteUpdated(Auction auction, Quote quote) {
        // Update price monitor with new prices information
        // TODO: Make sure this only happens every 10 seconds when the price perturbs!
        monitor.addQuote(quote.getAskPrice());
        refreshBid();
    }

    public void auctionBidUpdated(Auction auction, BidString bidString) {
        // If this is called when a bid is accepted,
        // then this checks if we have a waiting bid to submit
        if (bidDirtyFlag) {
            refreshBid();
        }
    }

    public void auctionBidRejected(Auction auction, BidString bidString) {
        System.err.println("FlightAgent Bid rejected");
    }

    public void auctionBidError(Auction auction, BidString bidString, int error) {
        System.err.println("FlightAgent Bid error " + error);
    }

    public void auctionTransaction(Auction auction, List<Buyable> tickets) {
        // We got a flight ticket!
        for (Buyable ticket : tickets) {
            flightAgent.addTicket((FlightTicket)ticket);
        }
        System.out.println("FlightAgent got a ticket!");
        numWanted -= tickets.size();
        refreshBid();
    }

    public void auctionClosed(Auction auction) {
    }

    private void refreshBid() {
        // flag saying this bid must be refreshed when possible
        bidDirtyFlag = true;
        
        try {
            auction.wipeBid();
            if (numWanted > 0) {
                auction.modifyBidPoint(numWanted, calcPrice());
            }

            auction.submitBid();
        } catch (BidInUseException e) {
            // return early, don't unset bidDirtyFlag
            return;
        }

        bidDirtyFlag = false;
    }

    private float calcPrice() {
        List<Double> dist = monitor.priceCumulativeDist();
        // Find lowest price that has confidence within required bounds
        for (int price = 0; price < dist.size(); price ++) {
            double prob = dist.get(price);
            if (prob > BID_CONFIDENCE) {
                return (float)price;
            }
        }

        // If confidence required is too high, return highest price
        return (float)(dist.size() - 1);
    }
}
