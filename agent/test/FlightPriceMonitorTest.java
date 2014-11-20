package agent.test;

import agent.FlightPriceMonitor;
import agent.PlaneTicket;

import java.util.*;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import static org.junit.Assert.*;

public class FlightPriceMonitorTest {

	private static final float X_MIN = -10;
	private static final float X_MAX = 30;
	private static final float X_DIV = 1;

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

	@Test
	public void testPredictMinimum() {
		int repeats = 3;

		PlaneTicket ticket = new PlaneTicket(2, false);

		float overallTimeAcc = 0f;
		float overallPriceAcc = 0f;

		for (float x = X_MIN; x <= X_MAX; x += X_DIV) {
			float timeAcc = 0f;
			float priceAcc = 0f;

			for (int i = 0; i < repeats; i ++) {
				FlightPriceMonitor monitor = 
					new FlightPriceMonitor(ticket);
				PriceGenerator gen = new PriceGenerator(x);

				// Test prediction with no quotes given.
				// Shouldn't crash, but return value is not important.
				monitor.predictMinimumTime();
				monitor.predictMinimumPrice();

				List<Integer> timePredictions = new ArrayList<Integer>();
				List<Float> pricePredictions = new ArrayList<Float>();
				float minPrice = PriceGenerator.PRICE_MAX;
				int minTime = 0;
				int time = 0;

				while (gen.hasNext()) {
					float quote = gen.next();

					if (quote < minPrice) {
						minPrice = quote;
						minTime = time;
					}

					monitor.addQuote(quote);

					int timePrediction = monitor.predictMinimumTime();
					timePredictions.add(timePrediction);

					float pricePrediction = monitor.predictMinimumPrice();
					pricePredictions.add(pricePrediction);

					time ++;
				}

				// Calculate overall accuracy from predictions
				float thisTimeAcc = 0f;
				for (int timePrediction : timePredictions) {
					thisTimeAcc += 1.0f - 
						((float)Math.abs(timePrediction - minTime) / 
						(float)PriceGenerator.MAX_TIME);
				}
				thisTimeAcc /= PriceGenerator.MAX_TIME;
				timeAcc += thisTimeAcc;

				float thisPriceAcc = 0f;
				for (float pricePrediction : pricePredictions) {
					thisPriceAcc += 1.0f - 
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

class PriceGenerator implements Iterator<Float> {

	private static final float START_MIN = 250;
	private static final float START_MAX = 400;
	public static final float PRICE_MIN = 150;
	public static final float PRICE_MAX = 800;
	public static final float MAX_TIME = 54;

	private final float x;
	private int time = -1;
	private float lastPrice;
	private final Random random = new Random();

	public PriceGenerator(float x) {
		this.x = x;
	}

	public boolean hasNext() {
		return (time < MAX_TIME - 1);
	}

	public Float next() {
		if (time == -1) {
			// generate initial price
			time = 0;
			lastPrice = randRange(START_MIN, START_MAX);
			return lastPrice;
		} else {
			// perturb last price
			time ++;
			float f = xFunc();
			float diff = 0;
			if (f > 0) diff = randRange(-10f, f);
			else if (f < 0) diff = randRange(f, 10f);
			else diff = randRange(-10f, 10f);

			lastPrice += diff;
			if (lastPrice < PRICE_MIN) lastPrice = PRICE_MIN;
			if (lastPrice > PRICE_MAX) lastPrice = PRICE_MAX;
			return lastPrice;
		}
	}

	public void remove() {
		throw new UnsupportedOperationException();
	}

 	private float xFunc() {
		return 10f + ((float)time / (float)MAX_TIME) * (x - 10f);
	}

	private float randRange(float min, float max) {
		return min + (max - min) * random.nextFloat();
	}
}