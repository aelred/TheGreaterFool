package agent.test;

import agent.FlightPriceMonitor;
import agent.FlightAuction;

import java.util.*;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import static org.junit.Assert.*;

public class FlightPriceMonitorTest {

	private static final int X_MIN = -10;
	private static final int X_MAX = 30;

	@Test
	public void testPriceMonitor() {
		FlightAuction auction = new FlightAuction(3, true);
		FlightPriceMonitor monitor = new FlightPriceMonitor(auction);

		assertEquals(auction, monitor.getAuction());
	}

	@Test
	public void testAddQuote() {
		Double[] validStarts = {250d, 251d, 301d, 399d, 400d};
		Double[] validQuotes = {150d, 151d, 250d, 342d, 400d, 799d, 800d};
		Double[] invalidStarts = {-100d, 0d, 100d, 249d, 401d, 450d};
		Double[] invalidQuotes = {-42d, 0d, 40d, 149d, 801d, 1293d};

		FlightAuction auction = new FlightAuction(3, true);

		for (double start : validStarts) {
			FlightPriceMonitor monitor = new FlightPriceMonitor(auction);

			// Assert valid starting quotes are accepted
			monitor.addQuote(start);

			// Assert valid quotes are accepted
			for (double quote : validQuotes) {
				monitor.addQuote(quote);
			}

			// Assert invalid quotes throw an exception
			for (double quote : invalidQuotes) {
				boolean thrown = false;
				try {
					monitor.addQuote(quote);
				} catch (IllegalArgumentException e) {
					thrown = true;
				}
				assertTrue(thrown);
			}
		}

		for (double start : invalidStarts) {
			FlightPriceMonitor monitor = new FlightPriceMonitor(auction);

			boolean thrown = false;
			try {
				monitor.addQuote(start);
			} catch (IllegalArgumentException e) {
				thrown = true;
			}
			assertTrue(thrown);
		}
	}

	@Test
	public void testPredictMinimum() {
		int repeats = 3;

		FlightAuction auction = new FlightAuction(2, false);

		double overallTimeAcc = 0d;
		double overallPriceAcc = 0d;

		for (int x = X_MIN; x <= X_MAX; x ++) {
			double timeAcc = 0d;
			double priceAcc = 0d;

			for (int i = 0; i < repeats; i ++) {
				FlightPriceMonitor monitor = 
					new FlightPriceMonitor(auction);
				PriceGenerator gen = new PriceGenerator(x);

				// Test prediction with no quotes given.
				// Shouldn't crash, but return value is not important.
				monitor.predictMinimumTime();
				monitor.predictMinimumPrice();

				List<Integer> timePredictions = new ArrayList<Integer>();
				List<Double> pricePredictions = new ArrayList<Double>();
				double minPrice = PriceGenerator.PRICE_MAX;
				int minTime = 0;
				int time = 0;

				while (gen.hasNext()) {
					double quote = gen.next();

					if (quote < minPrice) {
						minPrice = quote;
						minTime = time;
					}

					monitor.addQuote(quote);

					int timePrediction = monitor.predictMinimumTime();
					timePredictions.add(timePrediction);

					double pricePrediction = monitor.predictMinimumPrice();
					pricePredictions.add(pricePrediction);

					time ++;
				}

				// Calculate overall accuracy from predictions
				double thisTimeAcc = 0d;
				for (int timePrediction : timePredictions) {
					thisTimeAcc += 1.0d - 
						((double)Math.abs(timePrediction - minTime) / 
						(double)PriceGenerator.MAX_TIME);
				}
				thisTimeAcc /= PriceGenerator.MAX_TIME;
				timeAcc += thisTimeAcc;

				double thisPriceAcc = 0d;
				for (double pricePrediction : pricePredictions) {
					thisPriceAcc += 1.0d - 
						(Math.abs(pricePrediction - minPrice) / 
						(PriceGenerator.PRICE_MAX - 
						 PriceGenerator.PRICE_MIN));
				}
				thisPriceAcc /= PriceGenerator.MAX_TIME;
				priceAcc += thisPriceAcc;
			}

			timeAcc /= repeats;
			overallTimeAcc += timeAcc;
			priceAcc /= repeats;
			overallPriceAcc += priceAcc;

			System.out.print(x);
			System.out.print(", ");
			System.out.print(timeAcc);
			System.out.print(", ");
			System.out.println(priceAcc);
		}

		overallTimeAcc /= X_MAX - X_MIN;
		overallPriceAcc /= X_MAX - X_MIN;

		System.out.println("Overall accuracy");
		System.out.println(overallTimeAcc);
		System.out.println(overallPriceAcc);

		// Time accuracy must be better than 80%
		assertTrue(overallTimeAcc > 0.8);
		// Price accuracy must be better than 95%
		assertTrue(overallPriceAcc > 0.95);
	}
}

class PriceGenerator implements Iterator<Double> {

	private static final double START_MIN = 250;
	private static final double START_MAX = 400;
	public static final double PRICE_MIN = 150;
	public static final double PRICE_MAX = 800;
	public static final double MAX_TIME = 54;

	private final int x;
	private int time = -1;
	private double lastPrice;
	private final Random random = new Random();

	public PriceGenerator(int x) {
		this.x = x;
	}

	public boolean hasNext() {
		return (time < MAX_TIME - 1);
	}

	public Double next() {
		if (time == -1) {
			// generate initial price
			time = 0;
			lastPrice = randRange(START_MIN, START_MAX);
			return lastPrice;
		} else {
			// perturb last price
			time ++;
			double f = xFunc();
			double diff = 0;
			if (f > 0) diff = randRange(-10d, f);
			else if (f < 0) diff = randRange(f, 10d);
			else diff = randRange(-10d, 10d);

			lastPrice += diff;
			if (lastPrice < PRICE_MIN) lastPrice = PRICE_MIN;
			if (lastPrice > PRICE_MAX) lastPrice = PRICE_MAX;
			return lastPrice;
		}
	}

	public void remove() {
		throw new UnsupportedOperationException();
	}

 	private double xFunc() {
		return 10d + ((double)time / (double)MAX_TIME) * ((double)x - 10d);
	}

	private double randRange(double min, double max) {
		return min + (max - min) * random.nextDouble();
	}
}