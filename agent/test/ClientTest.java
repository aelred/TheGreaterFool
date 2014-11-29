package agent.test;

import agent.Client;
import agent.EntertainmentType;
import agent.Package;

import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import static org.junit.Assert.*;

public class ClientTest {
	@Test
	public void testAllPossiblePackages() {
		Client client = new DummyClient(1, 3, 100);

		int lastUtility = Integer.MAX_VALUE;
		for (Package pack : client.allPossiblePackages()) {
			assertTrue(pack.isFeasible());
			assertTrue(pack.clientUtility() <= lastUtility);
			lastUtility = pack.clientUtility();
		}
	}
}

class DummyClient extends Client {
	private int in, out, premium;

	public DummyClient(int in, int out, int premium) {
		this.in = in;
		this.out = out;
		this.premium = premium;
	}

	public int getPreferredArrivalDay() {
		return this.in;
	}

	public int getPreferredDepartureDay() {
		return this.out;
	}

	public int getHotelPremium() {
		return this.premium;
	}

	public int getEntertainmentPremium(EntertainmentType type) {
		return 0;
	}
}