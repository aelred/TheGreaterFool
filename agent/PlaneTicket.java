package agent;

public class PlaneTicket extends Buyable {
    private final boolean outgoing;

    public boolean getOutgoing() {
        return outgoing;
    }

    public PlaneTicket(int day, boolean outgoing) throws IllegalArgumentException {
        super(day);

        // Day must be between 1st and 2nd-to-last day if outgoing
        // or between 2nd and last day if incoming
        if ((outgoing && day > Agent.NUM_DAYS-2) || (!outgoing && day < 1)) {
            throw new IllegalArgumentException("Day not within expected range.");
        }
        
        this.outgoing = outgoing;
    }
}
