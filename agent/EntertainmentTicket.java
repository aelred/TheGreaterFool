package agent;

/** Represents a ticket for an entertainment event on a particular day. */
public class EntertainmentTicket extends Buyable {
    private final EntertainmentType type;

    /** @return the type of entertainment the ticket is for. */
    public EntertainmentType getType() { return type; }

    public EntertainmentTicket(int day, EntertainmentType type) {
        super(day);
        this.type = type;
    }

    @Override
    public String toString() {
        return String.format("Ticket to %s on day %d", type, day);
    }
}
