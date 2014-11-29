package agent;

import se.sics.tac.aw.BidString;
import se.sics.tac.aw.Quote;
import se.sics.tac.aw.TACAgent;
import se.sics.tac.aw.Transaction;

import java.util.Set;

public abstract class Auction {
    protected int day;

    private Set<Watcher> watchers;

    public Auction(int day) {
        this.day = day;
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
        for (Watcher watcher : watchers) {
            watcher.auctionBidUpdated(this, bidString);
        }
    }

    public void fireBidRejected(BidString bidString) {
        for (Watcher watcher : watchers) {
            watcher.auctionBidRejected(this, bidString);
        }
    }

    public void fireBidError(BidString bidString, int error) {
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
}
