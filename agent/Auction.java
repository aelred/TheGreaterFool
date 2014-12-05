package agent;

import se.sics.tac.aw.BidString;
import se.sics.tac.aw.Quote;
import se.sics.tac.aw.TACAgent;
import se.sics.tac.aw.Transaction;

import java.util.*;

public abstract class Auction<T extends Buyable> {
    private Set<Watcher> watchers = new HashSet<Watcher>();
    private BidMap workingBids, activeBids;
    private boolean awaitingConfirmation = false;
    private TACAgent agent;
    private Quote mostRecentQuote;
    
    protected int day;
    
    public Auction(TACAgent agent, int day) {
        this.agent = agent;
        this.day = day;
        workingBids = new BidMap();
    }

    public int getDay() {
    	return day;
    }
    
    protected abstract int getAuctionID();

    protected int getAuctionID(int category, int type) {
        return TACAgent.getAuctionFor(category, type, day);
    }

    // Return the Buyable associated with this auction
    protected abstract T getBuyable();

    public void addWatcher(Watcher watcher) {
        watchers.add(watcher);
    }

    public void removeWatcher(Watcher watcher) {
        watchers.remove(watcher);
    }

    public void fireQuoteUpdated(Quote quote) {
        mostRecentQuote = quote;
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
        List<Buyable> buyables = new ArrayList<Buyable>();
        for (int i = 0; i < transaction.getQuantity(); i ++) {
            buyables.add(getBuyable());
        }

        for (Watcher watcher : watchers) {
            watcher.auctionTransaction(this, buyables);
            if (buyables.isEmpty()) break;
        }
    }

    public void fireClosed() {
        for (Watcher watcher : watchers) {
            watcher.auctionClosed(this);
        }
    }

    public interface Watcher {
        public void auctionQuoteUpdated(Auction<?> auction, Quote quote);
        public void auctionBidUpdated(Auction<?> auction, BidString bidString);
        public void auctionBidRejected(Auction<?> auction, BidString bidString);
        public void auctionBidError(Auction<?> auction, BidString bidString, int error);

        /** Called when one or more {@link agent.Buyable}s are acquired through the Auction.
         *
         * @param buyables A {@link java.util.List} of {@link agent.Buyable}s won. {@link agent.Auction.Watcher}s may
         *                 remove {@link agent.Buyable}s from this list to indicate that they have 'taken' them.
         */
        public void auctionTransaction(Auction<?> auction, List<Buyable> buyables);

        public void auctionClosed(Auction<?> auction);
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
        submitBid(false);
    }
    
    public void submitBid(boolean copyToConsole) throws BidInUseException {
    	if (awaitingConfirmation)
            throw new BidInUseException();
        BidString bs = generateBidString();
        awaitingConfirmation = true;
        if (copyToConsole)
        	Agent.logMessage("auction" + Integer.toString(getAuctionID()), "Submitting: " + bs.getBidString());
        agent.submitBid(bs);
    }
    
    private BidString generateBidString() {
        BidString bid = new BidString(getAuctionID());
        for (Float price : workingBids.keySet()) {
            try {
            	bid.addBidPoint(workingBids.get(price), price);
            } catch (Exception e) {
            	Agent.logMessage("auction" + Integer.toString(getAuctionID()), "Existing bid: "
            			+ bid.getBidString());
            	Agent.logMessage("auction" + Integer.toString(getAuctionID()), "Tried to add bid point: ("
            			+ Integer.toString(workingBids.get(price)) + " " + Float.toString(price) + ")");
            	throw e;
            }
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
    public static final long serialVersionUID = 1L;

    public BidMap(BidMap workingBids) {
        super(workingBids);
    }

    public BidMap() {
        super();
    }
}

class BidInUseException extends Exception {
    public static final long serialVersionUID = 1L;
}
