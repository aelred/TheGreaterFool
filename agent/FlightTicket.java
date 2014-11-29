package agent;

public class FlightTicket extends Buyable {
    private final boolean outgoing;

    public boolean getOutgoing() {
        return outgoing;
    }

    public FlightTicket(int day, boolean outgoing) throws IllegalArgumentException {
        super(day);

        // Day must be between 1st and 2nd-to-last day if outgoing
        // or between 2nd and last day if incoming
        if ((outgoing && day > Agent.NUM_DAYS-1) || (!outgoing && day < 2)) {
            throw new IllegalArgumentException(
                "Day not within expected range: " + day);
        }

        this.outgoing = outgoing;
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj) && (outgoing == ((FlightTicket)obj).outgoing);
    }

    @Override
    public int hashCode() {
        return day * 2 + (outgoing? 1 : 0);
    }
}
