package agent;

public class FlightTicket extends Buyable {
    private final boolean arrival;

    public boolean isArrival() {
        return arrival;
    }

    public FlightTicket(int day, boolean arrival) throws IllegalArgumentException {
        super(day);

        // Day must be between 1st and 2nd-to-last day if arrival
        // or between 2nd and last day if incoming
        if ((arrival && day > Agent.NUM_DAYS-1) || (!arrival && day < 2)) {
            throw new IllegalArgumentException(
                "Day not within expected range: " + day);
        }

        this.arrival = arrival;
    }
}
