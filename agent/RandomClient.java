package agent;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class RandomClient extends Client {
    private final int preferredArrivalDay, preferredDepartureDay;

    private final int hotelPremium;
    private final Map<EntertainmentType, Integer> entertainmentPremiums;

    public int getPreferredArrivalDay() { return preferredArrivalDay; }

    public int getPreferredDepartureDay() { return preferredDepartureDay; }

    public int getHotelPremium() { return hotelPremium; }

    public int getEntertainmentPremium(EntertainmentType type) {
        return entertainmentPremiums.get(type);
    }

    public RandomClient() {
        Random rnd = new Random();
        preferredArrivalDay = rnd.nextInt(5);
        preferredDepartureDay = rnd.nextInt(5 - preferredArrivalDay) + preferredArrivalDay;

        hotelPremium = rnd.nextInt(100) + 50;

        entertainmentPremiums = new HashMap<EntertainmentType, Integer>(3);
        entertainmentPremiums.put(EntertainmentType.ALLIGATOR_WRESTLING, rnd.nextInt(200));
        entertainmentPremiums.put(EntertainmentType.AMUSEMENT,           rnd.nextInt(200));
        entertainmentPremiums.put(EntertainmentType.MUSEUM,              rnd.nextInt(200));
    }
}
