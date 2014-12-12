package agent.entertainment;

import agent.Auction;
import agent.BidInUseException;
import agent.Buyable;
import agent.logging.AgentLogger;

import java.util.List;

public class Conman extends EntertainmentBidder {

    public static final int CON_SIZE = 56;
    public static final float CON_PRICE = 5000f;

    public Conman(EntertainmentAgent entAgent, EntertainmentAuction auction, AgentLogger logger) {
        super(entAgent, auction, logger);
        bid(CON_PRICE);
    }

    @Override
    protected void addBid() throws BidInUseException {
        auction.modifyBidPoint(-CON_SIZE, this.bidPrice);
    }

    @Override
    protected void removeBid() throws BidInUseException {
        auction.modifyBidPoint(CON_SIZE, this.bidPrice);
    }

    @Override
    public void auctionBuySuccessful(Auction<?> auction, List<Buyable> buyables) { }

    @Override
    public void auctionSellSuccessful(Auction<?> auction, int numSold) {
        logger.log("Made " + (CON_PRICE - 200) + " profit from some crazy fool.");
    }
}
