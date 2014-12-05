package agent;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import agent.hotel.HotelBooking;

public abstract class Client {
    public abstract int getPreferredArrivalDay();
    public abstract int getPreferredDepartureDay();
    public abstract int getHotelPremium();
    public abstract int getEntertainmentPremium(EntertainmentType type);

    private Package possiblePackage(int in, int out, boolean towers) {
    	Package pack = new Package(this, in, out);
		pack.setArrivalTicket(new FlightTicket(in, true));
		pack.setDepartureTicket(new FlightTicket(out, false));
		for (int day = in; day < out; day ++) {
			pack.setHotelBooking(new HotelBooking(day, towers));
		}
		return pack;
    }

    // Returns all possible packages for this client, sorted by utility
    public List<Package> allPossiblePackages() {
    	List<Package> packages = new ArrayList<Package>();

    	for (int in = 1; in <= Agent.NUM_DAYS; in ++) {
    		for (int out = in + 1; out <= Agent.NUM_DAYS; out ++) {
    			packages.add(possiblePackage(in, out, false));
    			packages.add(possiblePackage(in, out, true));
    		}
    	}

    	Collections.sort(packages, new PackageUtilityComparator());
        return packages;
    }
}

// Compare packages by utility
class PackageUtilityComparator implements Comparator<Package> {
    public int compare(Package p1, Package p2) {
        return p2.clientUtility() - p1.clientUtility();
    }
}
