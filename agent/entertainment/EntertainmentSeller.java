package agent.entertainment;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import agent.Agent;
import agent.Auction;
import agent.BidInUseException;
import agent.Buyable;

public class EntertainmentSeller extends EntertainmentBidder {
    public static final Logger log = Logger.getLogger(Agent.log.getName() + ".entertainment.seller");

    private final List<EntertainmentTicket> tickets = new ArrayList<EntertainmentTicket>(4);
    private float sellPrice;

    public EntertainmentSeller(EntertainmentAgent entAgent, List<EntertainmentTicket> tickets, float bidPrice) {
        super(entAgent, entAgent.agent.getEntertainmentAuction(tickets.get(0)));
        EntertainmentTicket firstTicket = tickets.get(0);
        for (EntertainmentTicket ticket : tickets) {
            if (ticket.getDay() != firstTicket.getDay() || ticket.getType() != firstTicket.getType()) {
                throw new IllegalArgumentException("EntertainmentSeller can only sell one type of ticket.");
            }
        }

        this.tickets.addAll(tickets);
        bid(bidPrice);
    }

    protected void addBid() throws BidInUseException {
        auction.modifyBidPoint(-tickets.size(), bidPrice);
    }

    protected void removeBid() throws BidInUseException {
        auction.modifyBidPoint(tickets.size(), bidPrice);
    }

    @Override
    public void auctionTransaction(Auction<?> auction, List<Buyable> buyables) {
        entAgent.ticketsSold(this, tickets);
    }
}
