package agent;

import se.sics.tac.aw.BidString;
import se.sics.tac.aw.Quote;
import se.sics.tac.aw.TACAgent;
import se.sics.tac.aw.Transaction;

import java.util.*;

public abstract class Auction {
    private final int auctionID;
    private Set<Watcher> watchers;
    private BidMap workingBids, activeBids; // Integer 1 is price, Integer 2 is quantity
    private boolean awaitingConfirmation = false;
    private TACAgent agent;
    
    protected int day;
    
    public Auction(TACAgent agent, int auctionID, int day) {
        this.agent = agent;
    	this.auctionID = auctionID;
        this.day = day;
        workingBids = new BidMap();
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
    
    public void wipeBid() throws BidInUseException {
    	if (awaitingConfirmation)
    		throw new BidInUseException();
    	workingBids = new BidMap();
    }
    
    public void modifyBidPoint(int quantity, float price) throws BidInUseException {
    	if (awaitingConfirmation)
    		throw new BidInUseException();
    	if (workingBids.containsKey(price)) {
    		workingBids.put(price, workingBids.get(price) + quantity);
    	} else {
    		workingBids.put(price, quantity);
    	}
    }
    
    public void submitBid() throws BidInUseException {
    	if (awaitingConfirmation)
    		throw new BidInUseException();
    	BidString bs = generateBidString();
    	awaitingConfirmation = true;
    	agent.submitBid(bs);
    }
    
    private BidString generateBidString() {
    	BidString bid = new BidString(auctionID);
    	for (Float price : workingBids.keySet()) {
    		bid.addBidPoint(workingBids.get(price), price);
    	}
    	return bid;
    }
    
    public BidMap getActiveBids() {
    	return activeBids;
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
