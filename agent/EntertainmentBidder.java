package agent;

import se.sics.tac.aw.BidString;
import se.sics.tac.aw.Quote;

import java.util.List;
import java.util.logging.Logger;

public class EntertainmentBidder implements EntertainmentAuction.Watcher {
    public static final Logger log = Logger.getLogger(EntertainmentBidder.class.getName());

    private final EntertainmentAgent entAgent;
    private final EntertainmentAuction auction;
    private final Package pkg;

    private float bidPrice;

    public EntertainmentAuction getAuction() { return auction; }
    public Package getPackage() { return pkg; }
    public float getBidPrice() { return bidPrice; }

    private void handleBidInUseException(BidInUseException ex) {
        log.warning(ex.getMessage());
        ex.printStackTrace();
    }

    public EntertainmentBidder(EntertainmentAgent entAgent, EntertainmentAuction auction, Package pkg, float bidPrice) {
        this.entAgent = entAgent;
        this.auction = auction;
        this.pkg = pkg;
        bid(bidPrice);
    }

    public void bid(float bidPrice) {
        try {
            auction.modifyBidPoint(1, bidPrice);
            auction.submitBid();
            this.bidPrice = bidPrice;
        } catch (BidInUseException ex) { handleBidInUseException(ex); }
    }

    public void cancelBid() {
        try {
            auction.modifyBidPoint(-1, bidPrice);
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
    public void auctionTransaction(Auction<?> auction, List<Buyable> buyables) {
        EntertainmentTicket ticket = (EntertainmentTicket)buyables.remove(buyables.size() - 1);
        pkg.setEntertainmentTicket(ticket);
        entAgent.ticketWon(this, ticket);
    }

    @Override
    public void auctionClosed(Auction<?> auction) {  }
}
