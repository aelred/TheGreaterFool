package agent;

import se.sics.tac.aw.BidString;
import se.sics.tac.aw.Quote;

import java.util.List;
import java.util.logging.Logger;

public class EntertainmentBuyer extends EntertainmentBidder {
    private final Package pkg;

    public EntertainmentAuction getAuction() { return auction; }
    public Package getPackage() { return pkg; }

    public EntertainmentBuyer(EntertainmentAgent entAgent, EntertainmentAuction auction, Package pkg, float bidPrice) {
        super(entAgent, auction);
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
