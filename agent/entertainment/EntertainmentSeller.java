package agent.entertainment;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import agent.Agent;
import agent.Auction;
import agent.BidInUseException;
import agent.Buyable;
import agent.logging.AgentLogger;

public class EntertainmentSeller extends EntertainmentBidder {

    private final List<EntertainmentTicket> tickets = new ArrayList<EntertainmentTicket>(4);
    private float sellPrice;

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
        List<EntertainmentTicket> soldTickets = tickets.subList(tickets.size() - numSold, tickets.size());
        tickets.removeAll(soldTickets);
        entAgent.ticketsSold(this, soldTickets);
    }
}
