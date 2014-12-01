package agent;

import se.sics.tac.aw.BidString;
import se.sics.tac.aw.Quote;
import se.sics.tac.aw.TACAgent;
import se.sics.tac.aw.Transaction;

import java.util.*;

public abstract class Auction {
    private Set<Watcher> watchers;
    private BidMap workingBids, activeBids; // Integer 1 is price, Integer 2 is quantity
    private boolean awaitingConfirmation = false;
    private TACAgent agent;
    private Quote mostRecentQuote;
    
    protected int day;
    
    public Auction(TACAgent agent, int day) {
        this.agent = agent;
        this.day = day;
        workingBids = new BidMap();
    }

    protected abstract int getAuctionID();

    protected int getAuctionID(int category, int type) {
        return agent.getAuctionFor(category, type, day);
    }

    public int getDay() {
        return day;
    }

    public void addWatcher(Watcher watcher) {
        watchers.add(watcher);
    }

    public void removeWatcher(Watcher watcher) {
        watchers.remove(watcher);
    }

    public void fireQuoteUpdated(Quote quote) {
        for (Watcher watcher : watchers) {
            watcher.auctionQuoteUpdated(this, quote);
        }
    }

    public void fireBidUpdated(BidString bidString) {
        activeBids = new BidMap(workingBids);
        awaitingConfirmation = false;
        for (Watcher watcher : watchers) {
            watcher.auctionBidUpdated(this, bidString);
        }
    }

    public void fireBidRejected(BidString bidString) {
        awaitingConfirmation = false;
        for (Watcher watcher : watchers) {
            watcher.auctionBidRejected(this, bidString);
        }
    }

    public void fireBidError(BidString bidString, int error) {
        awaitingConfirmation = false;
        for (Watcher watcher : watchers) {
            watcher.auctionBidError(this, bidString, error);
        }
    }

    public void fireTransaction(Transaction transaction) {
        for (Watcher watcher : watchers) {
            watcher.auctionTransaction(this, transaction);
        }
    }

    public void fireClosed() {
        for (Watcher watcher : watchers) {
            watcher.auctionClosed(this);
        }
    }

    public interface Watcher {
        public void auctionQuoteUpdated(Auction auction, Quote quote);
        public void auctionBidUpdated(Auction auction, BidString bidString);
        public void auctionBidRejected(Auction auction, BidString bidString);
        public void auctionBidError(Auction auction, BidString bidString, int error);
        public void auctionTransaction(Auction auction, Transaction transaction);
        public void auctionClosed(Auction auction);
    }

    /** Clear the bid string currently being built. Changes are not submitted until submitBid is called.
     *
     * @throws BidInUseException if the previously submitted bid has not yet been confirmed.
     */
    public void wipeBid() throws BidInUseException {
        if (awaitingConfirmation)
            throw new BidInUseException();
        workingBids = new BidMap();
    }

    /** Set a quantity the agent is willing to buy at a certain price. If a bid point has already been set for that
     * price, the quantity will be adjusted by the given amount. Changes are not submitted until submitBid is called.
     *
     * @param quantity The amount to adjust the quantity by. Negative values reduce the quantity being bid for.
     * @param price The price at which to buy or sell the item.
     * @throws BidInUseException if the previously submitted bid has not yet been confirmed.
     */
    public void modifyBidPoint(int quantity, float price) throws BidInUseException {
        if (awaitingConfirmation)
            throw new BidInUseException();
        if (workingBids.containsKey(price)) {
            workingBids.put(price, workingBids.get(price) + quantity);
        } else {
            workingBids.put(price, quantity);
        }
    }

    /** Submit the changes made to the bid string by wipeBid and modifyBidPoint.
     *
     * @throws BidInUseException if the previously submitted bid has not yet been confirmed.
     */
    public void submitBid() throws BidInUseException {
        if (awaitingConfirmation)
            throw new BidInUseException();
        BidString bs = generateBidString();
        awaitingConfirmation = true;
        agent.submitBid(bs);
    }
    
    private BidString generateBidString() {
        BidString bid = new BidString(getAuctionID());
        for (Float price : workingBids.keySet()) {
            bid.addBidPoint(workingBids.get(price), price);
        }
        return bid;
    }

    /** @return a {@link agent.BidMap} containing the agent's active bids in this auction. */
    public BidMap getActiveBids() {
        return activeBids;
    }
    
    public float getAskPrice() {
    	if (mostRecentQuote != null) {
    		return mostRecentQuote.getAskPrice();
    	} else {
    		return 0;
    	}
    }
    
    public Quote getMostRecentQuote() {
    	return mostRecentQuote;
    }
}

class BidMap extends HashMap<Float,Integer> {

    public BidMap(BidMap workingBids) {
        super(workingBids);
    }

    public BidMap() {
        super();
    }
}

class BidInUseException extends Exception {}
