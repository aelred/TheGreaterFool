package agent;
import se.sics.tac.aw.*;
import java.util.*;

public class FlightAgent extends SubAgent<FlightTicket> {

    private Map<FlightAuction, Integer> intentions = 
        new HashMap<FlightAuction, Integer>();
    private Map<FlightAuction, FlightPriceMonitor> monitors = 
        new HashMap<FlightAuction, FlightPriceMonitor>();

    public FlightAgent(Agent agent, List<FlightTicket> stock) {
        super(agent, stock);

        // create a price monitor for every flight auction
        for (int day = 1; day <= Agent.NUM_DAYS; day ++) {
            FlightAuction auctionOut = agent.getFlightAuction(day, true);
            FlightAuction auctionIn = agent.getFlightAuction(day, false);
            monitors.put(auctionOut, new FlightPriceMonitor(auctionOut));
            monitors.put(auctionIn, new FlightPriceMonitor(auctionIn));
        }
    }

    public void fulfillPackages(List<Package> packages) {
        for (Package pack : packages) {
            fulfillPackage(pack);
        }
    }

    private void addIntention(FlightAuction auction) {
        if (!intentions.containsKey(auction)) intentions.put(auction, 0);
        intentions.put(auction, intentions.get(auction) + 1);
    }

    private void fulfillPackage(Package pack) {
        addIntention(agent.getFlightAuction(pack.getArrivalDay(), true));
        addIntention(agent.getFlightAuction(pack.getDepartureDay(), false));
    }
}
