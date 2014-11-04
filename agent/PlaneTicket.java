package agent;

public class PlaneTicket {
    private int day;
    private boolean outgoing;

    public int getDay() {
        return day;
    }

    public boolean getOutgoing() {
        return outgoing;
    }

    public PlaneTicket(int day, boolean outgoing) throws IllegalArgumentException {
        // Day must be between 1st and 2nd-to-last day if outgoing
        // or between 2nd and last day if incoming
        if ((outgoing && (day < 0 || day > Agent.NUM_DAYS-2)) ||
            (!outgoing && (day < 1 || day > Agent.NUM_DAYS-1))) {
            throw new IllegalArgumentException("Day not within expected range.");
        }
        this.day = day;
        this.outgoing = outgoing;
    }
}
