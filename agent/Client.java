package agent;

import java.util.List;
import java.util.ArrayList;

public abstract class Client {
    public abstract int getPreferredArrivalDay();
    public abstract int getPreferredDepartureDay();
    public abstract int getHotelPremium();
    public abstract int getEntertainmentPremium(EntertainmentType type);

    private Package possiblePackage(int in, int out, boolean towers) {
    	Package pack = new Package(this, in, out);
		pack.setArrivalTicket(new PlaneTicket(in, true));
		pack.setDepartureTicket(new PlaneTicket(out, false));
		for (int day = in; day < out; day ++) {
			pack.setHotelBooking(new HotelBooking(day, towers));
		}
		return pack;
    }

    // Returns all possible packages for this client
    public List<Package> allPossiblePackages() {
    	List<Package> packages = new ArrayList<Package>();

    	for (int in = 0; in < Agent.NUM_DAYS; in ++) {
    		for (int out = in + 1; out < Agent.NUM_DAYS; out ++) {
    			packages.add(possiblePackage(in, out, false));
    			packages.add(possiblePackage(in, out, true));
    		}
    	}

    	return packages;
    }
}
