package agent;

public abstract class Client {
    public abstract int getPreferredArrivalDay();
    public abstract int getPreferredDepartureDay();
    public abstract int getHotelPremium();
    public abstract int getEntertainmentPremium(EntertainmentType type);
}
