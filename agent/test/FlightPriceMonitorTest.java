package agent.test;

import agent.FlightPriceMonitor;
import agent.PlaneTicket;

import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import static org.junit.Assert.*;

public class FlightPriceMonitorTest {
	@Test
	public void testPriceMonitor() {
		PlaneTicket ticket = new PlaneTicket(3, true);
		FlightPriceMonitor monitor = new FlightPriceMonitor(ticket);

		assertEquals(ticket, monitor.getTicket());
	}

	@Test
	public void testAddQuote() {
		Float[] validStarts = {250f, 251f, 301f, 399f, 400f};
		Float[] validQuotes = {150f, 151f, 250f, 342f, 400f, 799f, 800f};
		Float[] invalidStarts = {-100f, 0f, 100f, 249f, 401f, 450f};
		Float[] invalidQuotes = {-42f, 0f, 40f, 149f, 801f, 1293f};

		PlaneTicket ticket = new PlaneTicket(3, true);

		for (float start : validStarts) {
			FlightPriceMonitor monitor = new FlightPriceMonitor(ticket);

			// Assert valid starting quotes are accepted
			monitor.addQuote(start);

			// Assert valid quotes are accepted
			for (float quote : validQuotes) {
				monitor.addQuote(quote);
			}

			// Assert invalid quotes throw an exception
			for (float quote : invalidQuotes) {
				boolean thrown = false;
				try {
					monitor.addQuote(quote);
				} catch (IllegalArgumentException e) {
					thrown = true;
				}
				assertTrue(thrown);
			}
		}

		for (float start : invalidStarts) {
			FlightPriceMonitor monitor = new FlightPriceMonitor(ticket);

			boolean thrown = false;
			try {
				monitor.addQuote(start);
			} catch (IllegalArgumentException e) {
				thrown = true;
			}
			assertTrue(thrown);
		}
	}
}