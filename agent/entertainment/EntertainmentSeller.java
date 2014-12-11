package agent.entertainment;

import agent.Auction;
import agent.BidInUseException;
import agent.Buyable;
import agent.logging.AgentLogger;

import java.util.ArrayList;
import java.util.List;

public class EntertainmentSeller extends EntertainmentBidder {

    private final List<EntertainmentTicket> tickets = new ArrayList<EntertainmentTicket>(4);

    public EntertainmentSeller(EntertainmentAgent entAgent, List<EntertainmentTicket> tickets, float bidPrice, 
    		AgentLogger logger) {
        super(entAgent, entAgent.agent.getEntertainmentAuction(tickets.get(0)), logger);
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
    public void auctionBuySuccessful(Auction<?> auction, List<Buyable> buyables) { }

    @Override
    public void auctionSellSuccessful(Auction<?> auction, int numSold) {
        List<EntertainmentTicket> soldTickets = new ArrayList<EntertainmentTicket>(numSold);
        for (int i = 0; i < numSold; i++) {
            soldTickets.add(tickets.remove(tickets.size() - 1));
        }
        tickets.removeAll(soldTickets);
        entAgent.ticketsSold(this, soldTickets);
    }
}
