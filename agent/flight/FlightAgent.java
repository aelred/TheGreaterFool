package agent.flight;
import se.sics.tac.aw.*;

import java.util.*;
import java.util.logging.Logger;

import agent.*;
import agent.logging.AgentLogger;

public class FlightAgent extends SubAgent<FlightTicket> {

    public static final double PRICE_MIN = 150;
    public static final double PRICE_MAX = 800;
    public static final int MAX_TIME = 54;

    private Map<FlightAuction, FlightBidder> bidders = 
        new HashMap<FlightAuction, FlightBidder>();

    private final List<FlightTicket> unusedStock = 
        new ArrayList<FlightTicket>();

    public FlightAgent(Agent agent, List<FlightTicket> stock, AgentLogger logger) {
        super(agent, stock, logger);

        // Add stock to unused stock
        for (FlightTicket ticket : stock) {
            unusedStock.add(ticket);
        }

        // Create a bidder for every auction
        for (int day = 1; day < Agent.NUM_DAYS; day ++) {
            FlightAuction auctionOut = agent.getFlightAuction(day, true);
            FlightAuction auctionIn = agent.getFlightAuction(day+1, false);
            bidders.put(auctionOut, new FlightBidder(this, auctionOut, logger.getSublogger("outBidder")));
            bidders.put(auctionIn, new FlightBidder(this, auctionIn, logger.getSublogger("inBidder")));
        }
    }

    public void gameStopped() {
        // Tell bidders to stop
        logger.log("Stopping FlightAgent");
        for (FlightBidder bidder : bidders.values()) {
            bidder.gameStopped();
        }
    }

    public float purchaseProbability(Auction<?> auction) {
        // Assume purchase is certain
        // TODO: be less naive?
        return 1f;
    }

    public void clearPackages() {
        // Tell bidders to clear packages
        logger.log("Clearing packages");
        for (FlightBidder bidder : bidders.values()) {
            bidder.clearPackages();
        }
    }

    public void fulfillPackages(List<agent.Package> packages) {
        for (agent.Package pack : packages) {
            fulfillPackage(pack);
        }
    }

    public void addTicket(FlightTicket ticket) {
        logger.log("Got ticket " + ticket.toString());
        this.stock.add(ticket);
    }

    private void fulfillPackage(agent.Package pack) {
        logger.log("Fullfill package");
        logger.log("Arrival: " + pack.getArrivalDay());
        logger.log("Departure: " + pack.getDepartureDay());

        // Check if arrival ticket already in unused stock
        FlightTicket arrival = null;
        for (FlightTicket ticket : unusedStock) {
            if (ticket.isArrival() && 
                ticket.getDay() == pack.getArrivalDay()) {
                arrival = ticket;
                break;
            }
        }

        if (arrival != null) {
            // Use unused ticket
            logger.log("Using unused arrival ticket");
            pack.setArrivalTicket(arrival);
            unusedStock.remove(arrival);
        } else {
            // Start bidding for ticket
            logger.log("Bidding for arrival ticket");
            bidders.get(agent.getFlightAuction(pack.getArrivalDay(), true))
                .addPackage(pack);
        }

        // Check if departure ticket already in unused stock
        FlightTicket departure = null;
        for (FlightTicket ticket : unusedStock) {
            if (!ticket.isArrival() && 
                ticket.getDay() == pack.getDepartureDay()) {
                departure = ticket;
                break;
            }
        }

        if (departure != null) {
            // Use unused ticket
            logger.log("Using unused departure ticket");
            pack.setDepartureTicket(departure);
            unusedStock.remove(departure);
        } else {
            // Start bidding for ticket
            logger.log("Bidding for departure ticket");
            bidders.get(agent.getFlightAuction(pack.getDepartureDay(), false))
                .addPackage(pack);
        }
    }
}
