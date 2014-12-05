package agent;

import se.sics.tac.aw.BidString;
import se.sics.tac.aw.Quote;

import java.util.List;
import java.util.logging.Logger;

public abstract class EntertainmentBidder implements EntertainmentAuction.Watcher {
    public static final Logger log = Logger.getLogger(Agent.log.getName() + ".entertainment.bidder");
    protected final EntertainmentAgent entAgent;
    protected final EntertainmentAuction auction;
    protected float bidPrice;

    public EntertainmentBidder(EntertainmentAgent entAgent, EntertainmentAuction auction) {
        this.entAgent = entAgent;
        this.auction = auction;
    }

    protected void handleBidInUseException(BidInUseException ex) {
        EntertainmentBidder.log.warning(ex.getMessage());
        ex.printStackTrace();
    }

    public float getBidPrice() { return bidPrice; }

    protected abstract void addBid() throws BidInUseException;
    protected abstract void removeBid() throws BidInUseException;

    public void bid(float bidPrice) {
        try {
            addBid();
            auction.submitBid();
            this.bidPrice = bidPrice;
        } catch (BidInUseException ex) { handleBidInUseException(ex); }
    }

    public void cancelBid() {
        try {
            removeBid();
            auction.submitBid();
        } catch (BidInUseException ex) { handleBidInUseException(ex); }
    }

    @Override
    public void auctionQuoteUpdated(Auction<?> auction, Quote quote) {  }

    @Override
    public void auctionBidUpdated(Auction<?> auction, BidString bidString) {  }

    @Override
    public void auctionBidRejected(Auction<?> auction, BidString bidString) {  }

    @Override
    public void auctionBidError(Auction<?> auction, BidString bidString, int error) {  }

    @Override
    public abstract void auctionTransaction(Auction<?> auction, List<Buyable> buyables);

    @Override
    public void auctionClosed(Auction<?> auction) {  }
}
