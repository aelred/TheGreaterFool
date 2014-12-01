package agent;
import se.sics.tac.aw.*;
import java.util.*;
import java.util.logging.Logger;

public class FlightAgent extends SubAgent<FlightTicket> {

    public static final Logger log = 
        Logger.getLogger(Agent.log.getName() + ".flights");

    private Map<FlightAuction, FlightBidder> bidders = 
        new HashMap<FlightAuction, FlightBidder>();

    public FlightAgent(Agent agent, List<FlightTicket> stock) {
        super(agent, stock);

        // Create a bidder for every auction
        for (int day = 1; day < Agent.NUM_DAYS; day ++) {
            FlightAuction auctionOut = agent.getFlightAuction(day, true);
            FlightAuction auctionIn = agent.getFlightAuction(day+1, false);
            bidders.put(auctionOut, new FlightBidder(this, auctionOut));
            bidders.put(auctionIn, new FlightBidder(this, auctionIn));
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
        bidders.get(agent.getFlightAuction(pack.getArrivalDay(), true)).addWanted();
        bidders.get(agent.getFlightAuction(pack.getDepartureDay(), false)).addWanted();
    }
}
