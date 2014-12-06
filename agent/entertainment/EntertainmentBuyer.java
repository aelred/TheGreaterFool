package agent.entertainment;

import se.sics.tac.aw.BidString;
import se.sics.tac.aw.Quote;

import java.util.List;
import java.util.logging.Logger;

import agent.Auction;
import agent.BidInUseException;
import agent.Buyable;
import agent.Package;
import agent.logging.AgentLogger;

public class EntertainmentBuyer extends EntertainmentBidder {
    private final Package pkg;

    public EntertainmentAuction getAuction() { return auction; }
    public Package getPackage() { return pkg; }

    public EntertainmentBuyer(EntertainmentAgent entAgent, EntertainmentAuction auction, Package pkg, float bidPrice,
    		AgentLogger logger) {
        super(entAgent, auction, logger);
        this.pkg = pkg;
        auction.addWatcher(this);
        bid(bidPrice);
    }

    protected void addBid() throws BidInUseException {
        auction.modifyBidPoint(1, bidPrice);
    }

    protected void removeBid() throws BidInUseException {
        auction.modifyBidPoint(-1, bidPrice);
    }

    @Override
    public void auctionTransaction(Auction<?> auction, List<Buyable> buyables) {
        EntertainmentTicket ticket = (EntertainmentTicket)buyables.remove(buyables.size() - 1);
        pkg.setEntertainmentTicket(ticket);
        entAgent.ticketWon(this, ticket);
    }

}
