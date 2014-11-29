package agent;

import java.util.Map;
import java.util.TreeMap;

/**
 * Represents the actual package being given to a client, as opposed to their preferences.
 */
public class Package {
    private final Client client;
    private int arrivalDay, departureDay;
    private PlaneTicket arrivalTicket, departureTicket;
    private final HotelBooking[] hotelBookings = new HotelBooking[4];
    private final Map<EntertainmentType, EntertainmentTicket> entertainmentTickets = new TreeMap<EntertainmentType,
                EntertainmentTicket>();

    public Client getClient() { return client; }

    public int getArrivalDay() { return arrivalDay; }
    public void setArrivalDay(int arrivalDay) { this.arrivalDay = arrivalDay; }

    public int getDepartureDay() { return departureDay; }
    public void setDepartureDay(int departureDay) { this.departureDay = departureDay; }

    public PlaneTicket getArrivalTicket() { return arrivalTicket; }
    public PlaneTicket getDepartureTicket() { return departureTicket; }

    public void setArrivalTicket(PlaneTicket arrivalTicket) {
        if (arrivalTicket.getDay() != arrivalDay) {
            throw new IllegalArgumentException("The given arrival ticket does not match the package arrival day.");
        }
        this.arrivalTicket = arrivalTicket;
    }
    public void setDepartureTicket(PlaneTicket departureTicket) {
        if (departureTicket.getDay() != departureDay) {
            throw new IllegalArgumentException("The given departure ticket does not match the package departure day.");
        }
        this.departureTicket = departureTicket;
    }

    public HotelBooking getHotelBooking(int day) { return hotelBookings[day - 1]; }
    public void setHotelBooking(HotelBooking booking) {
        // TODO: do something if there's already a booking in that slot
        hotelBookings[booking.getDay() - 1] = booking;
    }

    public EntertainmentTicket getEntertainmentTicket(EntertainmentType type) {
        return entertainmentTickets.get(type);
    }
    public void setEntertainmentTicket(EntertainmentTicket ticket) {
        // TODO: do something if there's already a booking in that slot
        entertainmentTickets.put(ticket.getType(), ticket);
    }
    public void clearEntertainmentTickets() {
        entertainmentTickets.clear();
    }

    /** Create a new Package with the given {@link agent.Client}, using the client's preferred dates. */
    public Package(Client client) {
        this(client, client.getPreferredArrivalDay(), client.getPreferredDepartureDay());
    }

    public Package(Client client, int arrivalDay, int departureDay) {
        this.client = client;
        this.arrivalDay = arrivalDay;
        this.departureDay = departureDay;
    }

    // TODO: `validate` function

    public boolean isFeasible() {
        if (getArrivalTicket() == null || getDepartureTicket() == null) {
            return false;
        }

        for (int day = arrivalDay; day < departureDay; day++) {
            if (getHotelBooking(day) == null) return false;
        }

        return true;
    }

    public boolean isEntertainmentPackageFeasible() {
        for (EntertainmentTicket ticket : entertainmentTickets.values()) {
            if (!(arrivalDay <= ticket.getDay() && ticket.getDay() < departureDay)) {
                return false;
            }
        }

        return true;
    }

    public boolean hasTampaTowersBonus() {
        for (int day = arrivalDay; day < departureDay; day++) {
            if (getHotelBooking(day) == null || !getHotelBooking(day).towers) return false;
        }

        return true;
    }

    // Utility calculation //

    public int travelPenalty() {
        return 100 * (  Math.abs(arrivalDay   - client.getPreferredArrivalDay())
                      + Math.abs(departureDay - client.getPreferredDepartureDay()));
    }

    public int hotelBonus() {
        return hasTampaTowersBonus() ? client.getHotelPremium() : 0;
    }

    public int funBonus() {
        int funBonus = 0;
        for (EntertainmentTicket ticket : entertainmentTickets.values()) {
            funBonus += client.getEntertainmentPremium(ticket.getType());
        }
        return funBonus;
    }

    public int clientUtility() {
        return isFeasible() ? 1000 - travelPenalty() + hotelBonus() + funBonus() : 0;
    }
}
