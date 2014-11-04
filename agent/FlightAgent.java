package agent;
import se.sics.tac.aw.*;
import java.util.*;

public class FlightAgent {

    private Set<PlaneTicket> tickets = new HashSet<PlaneTicket>();
    private Map<PlaneTicket, PriceMonitor> monitors = new HashMap<PlaneTicket, PriceMonitor>();

    public FlightAgent() {
        // create a price monitor for every flight auction
        for (int day = 0; day < Agent.NUM_DAYS; day ++) {
            PlaneTicket ticketOut = new PlaneTicket(day, true);
            PlaneTicket ticketIn = new PlaneTicket(day, false);
            monitors.put(ticketOut, new PriceMonitor(ticketOut));
            monitors.put(ticketIn, new PriceMonitor(ticketIn));
        }
    }
}

class PriceMonitor {

    // The plane ticket price to monitor
    private PlaneTicket ticket;

    // Historic prices
    private List<Integer> prices = new ArrayList<Integer>();

    public PriceMonitor(PlaneTicket ticket) {
        this.ticket = ticket;
    }
}
