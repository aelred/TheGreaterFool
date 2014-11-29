package agent;
import se.sics.tac.aw.*;
import java.util.*;

public class FlightAgent {

    private Set<FlightTicket> tickets = new HashSet<FlightTicket>();
    private Map<FlightTicket, FlightPriceMonitor> monitors = new HashMap<FlightTicket, FlightPriceMonitor>();

    public FlightAgent() {
        // create a price monitor for every flight auction
        for (int day = 1; day <= Agent.NUM_DAYS; day ++) {
            FlightTicket ticketOut = new FlightTicket(day, true);
            FlightTicket ticketIn = new FlightTicket(day, false);
            monitors.put(ticketOut, new FlightPriceMonitor(ticketOut));
            monitors.put(ticketIn, new FlightPriceMonitor(ticketIn));
        }
    }
}
