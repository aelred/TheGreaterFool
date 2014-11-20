package agent;

import java.util.*;

public class FlightPriceMonitor {

    private static final float START_MIN = 250;
    private static final float START_MAX = 400;
    private static final float PRICE_MIN = 150;
    private static final float PRICE_MAX = 800;

    private static final float X_MIN = -10;
    private static final float X_MAX = 30;
    private static final float X_DIV = 1;

    private static final int MAX_TIME = 54;

    // The plane ticket price to monitor
    private final PlaneTicket ticket;

    // Historic prices
    private final List<Float> prices = new ArrayList<Float>();

    // Estimates of the X constant affecting prices
    private final Map<Float, Float> probX = new HashMap<Float, Float>();

    public FlightPriceMonitor(PlaneTicket ticket) {
        this.ticket = ticket;

        // Set initial probabilities using probability density
        float initX = 1f / (X_MAX - X_MIN);

        for (float x = X_MIN; x < X_MAX; x += X_DIV) {
            probX.put(x, initX);
        }
    }

    public PlaneTicket getTicket() {
        return ticket;
    }
    
    public void addQuote(float quote) throws IllegalArgumentException {
        // Test quote is within expected range
        float min = PRICE_MIN;
        float max = PRICE_MAX;
        if (prices.size() == 0) {
            min = START_MIN;
            max = START_MAX;
        }

        if (quote < min || quote > max) {
            throw new IllegalArgumentException(
                "Price quote outside expected range");
        }

        // Take change in prices to update estimate
        if (prices.size() != 0) {
            updateEstimate(quote - prices.get(prices.size()-1));
        }
        prices.add(quote);
    }

    private int time() {
        return prices.size();
    }

    private float xFunc(float x) {
        return 10f + (x - 10f) * time() / MAX_TIME;
    }

    private float probDiffGivenX(float diff, float x) {
        float f = xFunc(x);

        float dmin = f;
        float dmax = f;

        if (f >= 0) dmin = -10;
        if (f <= 0) dmax = 10;

        if (dmin <= diff && diff <= dmax) {
            return 1f / (dmax - dmin);  // Probability density
        } else {
            return 0f;  // Out of range, impossible
        }
    }

    private void updateEstimate(float diff) {
        // Use Baye's theorem to improve estimates

        // Find probability of quote occuring for every x
        Map<Float, Float> probDiff = new HashMap<Float, Float>();
        float probDiffAll = 0f;

        for (float x = X_MIN; x < X_MAX; x += X_DIV) {
            float prob = probDiffGivenX(diff, x) * probX.get(x);
            probDiff.put(x, prob);
            probDiffAll += prob;
        }

        // Update probabilities
        for (float x = X_MIN; x < X_MAX; x += X_DIV) {
            probX.put(x, probDiff.get(x) / probDiffAll);
        }
    }
}
