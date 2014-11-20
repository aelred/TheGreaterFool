package agent;

import java.util.*;

public class FlightPriceMonitor {

    private static final float START_MIN = 250;
    private static final float START_MAX = 400;
    private static final float PRICE_MIN = 150;
    private static final float PRICE_MAX = 800;
    private static final float PRICE_DIV = 1;

    private static final float X_MIN = -10;
    private static final float X_MAX = 30;
    private static final float X_DIV = 1;

    private static final int MAX_TIME = 54;

    // The plane ticket price to monitor
    private final PlaneTicket ticket;

    // Historic prices
    private final List<Float> prices = new ArrayList<Float>();

    // Estimates of the X constant affecting prices
    private final Map<Float, Double> probX = new HashMap<Float, Double>();

    public FlightPriceMonitor(PlaneTicket ticket) {
        this.ticket = ticket;

        // Set initial probabilities using probability density
        double initX = 1f / (X_MAX - X_MIN);

        for (float x = X_MIN; x <= X_MAX; x += X_DIV) {
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

    public int predictMinimumTime() {
        List<Float> projection = projectPrices();
        return projection.indexOf(Collections.min(projection));
    }

    public float predictMinimumPrice() {
        return Collections.min(projectPrices());
    }

    private int time() {
        return prices.size();
    }

    private float xFunc(float x) {
        return xFunc(x, time());
    }

    private float xFunc(float x, int t) {
        return 10f + ((float)t - (float)MAX_TIME) * (x - 10f);
    }

    private double probDiffGivenX(float diff, float x) {
        float f = xFunc(x);

        float dmin = f;
        float dmax = f;

        if (f >= 0) dmin = -10;
        if (f <= 0) dmax = 10;

        if (dmin <= diff && diff <= dmax) {
            return 1d / (dmax - dmin);  // Probability density
        } else {
            return 0f;  // Out of range, impossible
        }
    }

    private void updateEstimate(float diff) {
        // Use Baye's theorem to improve estimates

        // Find probability of quote occuring for every x
        Map<Float, Double> probDiff = new HashMap<Float, Double>();
        double probDiffAll = 0f;

        for (float x = X_MIN; x <= X_MAX; x += X_DIV) {
            double prob = probDiffGivenX(diff, x) * probX.get(x);
            probDiff.put(x, prob);
            probDiffAll += prob;
        }

        // Update probabilities
        for (float x = X_MIN; x <= X_MAX; x += X_DIV) {
            probX.put(x, probDiff.get(x) / probDiffAll);
        }
    }

    private float averagePriceDiff(float x, int t) {
        float f = xFunc(x, t);
        if (f > 0) return (f - 10f) / 2f;
        else return (f + 10f) / 2f;
    }

    private List<Float> projectPrices(float x) {
        // project future price given x
        List<Float> futPrices = new ArrayList<Float>(prices);

        float last;
        if (time() != 0) {
            last = futPrices.get(futPrices.size()-1);
        } else {
            // Predict initial price
            last = (START_MAX - START_MIN) / 2f;
            futPrices.add(last);
        }

        for (int t = time(); t < MAX_TIME; t ++) {
            float price = last + averagePriceDiff(x, t);
            futPrices.add(price);
            last = price;
        }

        return futPrices;
    }

    private List<Float> projectPrices() {
        // project future prices over all x
        List<Float> futPrices = new ArrayList<Float>();
        for (int t = 0; t < MAX_TIME; t ++) {
            futPrices.add(0f);
        }

        for (float x : probX.keySet()) {
            List<Float> pricesX = projectPrices(x);
            // add prediction for this x, weighted by prob of x
            for (int t = 0; t < MAX_TIME; t ++) {
                float weighted = 
                    (float)((double)pricesX.get(t) * probX.get(x));
                futPrices.set(t, futPrices.get(t) + weighted);
            }
        }

        return futPrices;
    }
}
