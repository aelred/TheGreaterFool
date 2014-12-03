package agent;
import se.sics.tac.aw.*;
import java.util.*;
import java.util.logging.Logger;

public class FlightAgent extends SubAgent<FlightTicket> {

    public static final double PRICE_MIN = 150;
    public static final double PRICE_MAX = 800;
    public static final int MAX_TIME = 54;

    public static final Logger log = 
        Logger.getLogger(Agent.log.getName() + ".flights");

    private Map<FlightAuction, FlightBidder> bidders = 
        new HashMap<FlightAuction, FlightBidder>();

    private final List<FlightTicket> unusedStock = 
        new ArrayList<FlightTicket>();

    public FlightAgent(Agent agent, List<FlightTicket> stock) {
        super(agent, stock);

        // Add stock to unused stock
        for (FlightTicket ticket : stock) {
            unusedStock.add(ticket);
        }

        // Create a bidder for every auction
        for (int day = 1; day < Agent.NUM_DAYS; day ++) {
            FlightAuction auctionOut = agent.getFlightAuction(day, true);
            FlightAuction auctionIn = agent.getFlightAuction(day+1, false);
            bidders.put(auctionOut, new FlightBidder(this, auctionOut));
            bidders.put(auctionIn, new FlightBidder(this, auctionIn));
        }
    }

    public void gameStopped() {
        // Tell bidders to stop
        log.info("Stopping FlightAgent");
        for (FlightBidder bidder : bidders.values()) {
            bidder.gameStopped();
        }
    }

    public void fulfillPackages(List<Package> packages) {
        for (Package pack : packages) {
            fulfillPackage(pack);
        }
    }

    public void addTicket(FlightTicket ticket) {
        log.info("Got ticket " + ticket.toString());
        this.stock.add(ticket);
    }

    private void fulfillPackage(Package pack) {
        log.info("Fullfill package");
        log.info("Arrival: " + pack.getArrivalDay());
        log.info("Departure: " + pack.getDepartureDay());

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
            log.info("Using unused arrival ticket");
            pack.setArrivalTicket(arrival);
            unusedStock.remove(arrival);
        } else {
            // Start bidding for ticket
            log.info("Bidding for arrival ticket");
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
            log.info("Using unused departure ticket");
            pack.setDepartureTicket(departure);
            unusedStock.remove(departure);
        } else {
            // Start bidding for ticket
            log.info("Bidding for departure ticket");
            bidders.get(agent.getFlightAuction(pack.getDepartureDay(), false))
                .addPackage(pack);
        }
    }
}
