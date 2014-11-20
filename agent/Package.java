package agent;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the actual package being given to a client, as opposed to their preferences.
 */
public class Package {
    private final Client client;
    private int arrivalDay, departureDay;
    private PlaneTicket arrivalTicket, departureTicket;
    private final List<HotelBooking> hotelBookings = new ArrayList<HotelBooking>(4);
    private final List<EntertainmentTicket> entertainmentTickets = new ArrayList<EntertainmentTicket>(3);

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

    public List<HotelBooking> getHotelBookings() { return hotelBookings; }
    public List<EntertainmentTicket> getEntertainmentTickets() { return entertainmentTickets; }

    /** Create a new Package with the given {@link agent.Client}, using the client's preferred dates. */
    public Package(Client client) {
        this(client, client.getPreferredArrivalDay(), client.getPreferredDepartureDay());
    }

    public Package(Client client, int arrivalDay, int departureDay) {
        this.client = client;
        this.arrivalDay = arrivalDay;
        this.departureDay = departureDay;
    }

    // TODO: validate and calculateProfit functions
}
