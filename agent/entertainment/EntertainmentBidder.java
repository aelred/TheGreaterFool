package agent.entertainment;

import se.sics.tac.aw.BidString;
import se.sics.tac.aw.Quote;

import java.util.List;
import java.util.logging.Logger;

import agent.Agent;
import agent.Auction;
import agent.BidInUseException;
import agent.Buyable;
import agent.Auction.Watcher;
import agent.logging.AgentLogger;

public abstract class EntertainmentBidder implements EntertainmentAuction.Watcher {
    protected final EntertainmentAgent entAgent;
    protected final EntertainmentAuction auction;
    protected float bidPrice;
    protected final AgentLogger logger;

    private enum BidState { SUCCESSFUL, WAITING_TO_BID, WAITING_TO_CLEAR }
    private BidState bidState = BidState.SUCCESSFUL;

    public EntertainmentBidder(EntertainmentAgent entAgent, EntertainmentAuction auction, AgentLogger logger) {
        this.entAgent = entAgent;
        this.auction = auction;
        this.logger = logger;
        auction.addWatcher(this);
    }

    protected void handleBidInUseException(BidInUseException ex) {
        logger.log(ex.getMessage(),AgentLogger.WARNING);
        logger.logExceptionStack(ex, AgentLogger.WARNING);
    }

    public float getBidPrice() { return bidPrice; }

    protected abstract void addBid() throws BidInUseException;
    protected abstract void removeBid() throws BidInUseException;

    public void bid(float bidPrice) {
        this.bidPrice = bidPrice;
        try {
            addBid();
            auction.submitBid();
            bidState = BidState.SUCCESSFUL;
        } catch (BidInUseException ex) {
            bidState = BidState.WAITING_TO_BID;
        }
    }

    public void cancelBid() {
        try {
            removeBid();
            auction.submitBid();
            bidState = BidState.SUCCESSFUL;
        } catch (BidInUseException ex) {
            bidState = BidState.WAITING_TO_CLEAR;
        }
    }

    public void deregister() {
        cancelBid();
        auction.removeWatcher(this);
    }

    @Override
    public void auctionQuoteUpdated(Auction<?> auction, Quote quote) {  }

    @Override
    public void auctionBidUpdated(Auction<?> auction, BidString bidString) {
        switch (bidState) {
            case WAITING_TO_BID:
                bid(this.bidPrice);
                break;
            case WAITING_TO_CLEAR:
                cancelBid();
                break;
        }
    }

    @Override
    public void auctionBidRejected(Auction<?> auction, BidString bidString) {  }

    @Override
    public void auctionBidError(Auction<?> auction, BidString bidString, int error) {  }

    @Override
    public abstract void auctionBuySuccessful(Auction<?> auction, List<Buyable> buyables);

    @Override
    public abstract void auctionSellSuccessful(Auction<?> auction, int numSold);

    @Override
    public void auctionClosed(Auction<?> auction) {  }
}
