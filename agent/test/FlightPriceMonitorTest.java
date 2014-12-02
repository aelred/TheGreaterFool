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
		FlightAuction auction = new FlightAuction(null, 3, true);
		FlightPriceMonitor monitor = new FlightPriceMonitor(auction);

		assertEquals(auction, monitor.getAuction());
	}

	@Test
	public void testAddQuote() {
		Double[] validStarts = {250d, 251d, 301d, 399d, 400d};
		Double[] validQuotes = {150d, 151d, 250d, 342d, 400d, 799d, 800d};
		Double[] invalidStarts = {-100d, 0d, 100d, 249d, 401d, 450d};
		Double[] invalidQuotes = {-42d, 0d, 40d, 149d, 801d, 1293d};

		FlightAuction auction = new FlightAuction(null, 3, true);

		for (double start : validStarts) {
			FlightPriceMonitor monitor = new FlightPriceMonitor(auction);

			// Assert valid starting quotes are accepted
			monitor.addQuote(start, 0);

			// Assert valid quotes are accepted
            int t = 1;
			for (double quote : validQuotes) {
				monitor.addQuote(quote, t);
                t ++;
			}

			// Assert invalid quotes throw an exception
			for (double quote : invalidQuotes) {
				boolean thrown = false;
				try {
					monitor.addQuote(quote, t);
				} catch (IllegalArgumentException e) {
					thrown = true;
				}
				assertTrue(thrown);
                t ++;
			}
		}

		for (double start : invalidStarts) {
			FlightPriceMonitor monitor = new FlightPriceMonitor(auction);

			boolean thrown = false;
			try {
				monitor.addQuote(start, 0);
			} catch (IllegalArgumentException e) {
				thrown = true;
			}
			assertTrue(thrown);
		}
	}

	@Test
	public void testPredictMinimum() {
		int repeats = 3;

		FlightAuction auction = new FlightAuction(null, 2, false);

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
				monitor.predictMinimumTime(0);
				monitor.predictMinimumPrice(0);

				List<Integer> timePredictions = new ArrayList<Integer>();
				List<Double> pricePredictions = new ArrayList<Double>();
				int time = 0;

                // gives lowest price after this point 
                List<Double> rollingMinimum = new ArrayList<Double>();
                List<Integer> rollingMinTime = new ArrayList<Integer>();

				while (gen.hasNext()) {
					double quote = gen.next();

                    rollingMinimum.add(quote);
                    rollingMinTime.add(time);

					monitor.addQuote(quote, time);

					int timePrediction = monitor.predictMinimumTime(time);
					timePredictions.add(timePrediction);

					double pricePrediction = monitor.predictMinimumPrice(time);
					pricePredictions.add(pricePrediction);

					time ++;
				}

                // calculate rolling minimum backwards
                for (int j = time-2; j > 0; j --) {
                    if (rollingMinimum.get(j) > rollingMinimum.get(j+1)) {
                        // set this minimum to previous minimum
                        rollingMinimum.set(j, rollingMinimum.get(j+1));
                        rollingMinTime.set(j, rollingMinTime.get(j+1));
                    }
                }

				// Calculate overall accuracy from predictions
				double thisTimeAcc = 0d;
				double thisPriceAcc = 0d;
                for (int j = 0; j < time; j ++) {
					thisTimeAcc += 1.0d - 
						((double)Math.abs(timePredictions.get(j) - rollingMinTime.get(j)) / 
						PriceGenerator.MAX_TIME);
					thisPriceAcc += 1.0d - 
						(Math.abs(pricePredictions.get(j) - rollingMinimum.get(j)) / 
						(PriceGenerator.PRICE_MAX - 
						 PriceGenerator.PRICE_MIN));
                }
				thisTimeAcc /= time;
				timeAcc += thisTimeAcc;

				thisPriceAcc /= time;
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

    @Test
    public void testPriceCumulativeDist() {
        // Assert that probability begins at ~0d and increases to ~1d.
		FlightAuction auction = new FlightAuction(null, 3, true);

		for (int x = X_MIN; x <= X_MAX; x++) {
			FlightPriceMonitor monitor = new FlightPriceMonitor(auction);
            PriceGenerator gen = new PriceGenerator(x);

            int time = 0;

            while (gen.hasNext()) {
                double quote = gen.next();
                monitor.addQuote(quote, time);
                
                // Assert cumulative distribution is valid
                List<Double> dist = monitor.priceCumulativeDist(time);
                double lastProb = 0d;
                for (int price = 0; price < dist.size(); price ++) {
                    double prob = dist.get(price);
                    // Make sure probability always increases
                    assertTrue(prob >= lastProb);
                    lastProb = prob;
                }

                // Make sure final probability is approximately 1
                assertEquals(1d, lastProb, 0.01d);

                time ++;
            }
		}
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
		return 10d + ((double)time / MAX_TIME) * ((double)x - 10d);
	}

	private double randRange(double min, double max) {
		return min + (max - min) * random.nextDouble();
	}
}
