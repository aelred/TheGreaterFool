package agent;

import java.util.*;

public class FlightPriceMonitor {

    private static final double START_MIN = 250;
    private static final double START_MAX = 400;
    private static final double PRICE_MIN = 150;
    private static final double PRICE_MAX = 800;
    private static final double PRICE_DIV = 1;

    private static final int X_MIN = -10;
    private static final int X_MAX = 30;

    private static final int MAX_TIME = 54;

    // The plane ticket price to monitor
    private final FlightTicket ticket;

    // Historic prices
    private final List<Double> prices = new ArrayList<Double>();

    // Estimates of the X constant affecting prices
    private final Map<Integer, Double> probX = new HashMap<Integer, Double>();

    public FlightPriceMonitor(FlightTicket ticket) {
        this.ticket = ticket;

        // Set initial probabilities using probability density
        double initX = 1d / (double)(X_MAX - X_MIN);

        for (int x = X_MIN; x <= X_MAX; x ++) {
            probX.put(x, initX);
        }
    }

    public FlightTicket getTicket() {
        return ticket;
    }

    public void addQuote(double quote) throws IllegalArgumentException {
        // Test quote is within expected range
        double min = PRICE_MIN;
        double max = PRICE_MAX;
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
        List<Double> projection = projectPrices();
        return projection.indexOf(Collections.min(projection));
    }

    public double predictMinimumPrice() {
        return Collections.min(projectPrices());
    }

    private int time() {
        return prices.size();
    }

    private double xFunc(int x) {
        return xFunc(x, time());
    }

    private double xFunc(int x, int t) {
        return 10d + ((double)t - (double)MAX_TIME) * ((double)x - 10d);
    }

    private double probDiffGivenX(double diff, int x) {
        double f = xFunc(x);

        double dmin = f;
        double dmax = f;

        if (f >= 0) dmin = -10;
        if (f <= 0) dmax = 10;

        if (dmin <= diff && diff <= dmax) {
            return 1d / (dmax - dmin);  // Probability density
        } else {
            return 0d;  // Out of range, impossible
        }
    }

    private void updateEstimate(double diff) {
        // Use Baye's theorem to improve estimates

        // Find probability of quote occuring for every x
        Map<Integer, Double> probDiff = new HashMap<Integer, Double>();
        double probDiffAll = 0d;

        for (int x = X_MIN; x <= X_MAX; x ++) {
            double prob = probDiffGivenX(diff, x) * probX.get(x);
            probDiff.put(x, prob);
            probDiffAll += prob;
        }

        // Update probabilities
        for (int x = X_MIN; x <= X_MAX; x ++) {
            probX.put(x, probDiff.get(x) / probDiffAll);
        }
    }

    private double averagePriceDiff(int x, int t) {
        double f = xFunc(x, t);
        if (f > 0) return (f - 10d) / 2d;
        else return (f + 10d) / 2d;
    }

    private List<Double> projectPrices(int x) {
        // project future price given x
        List<Double> futPrices = new ArrayList<Double>(prices);

        double last;
        if (time() != 0) {
            last = futPrices.get(futPrices.size()-1);
        } else {
            // Predict initial price
            last = (START_MAX - START_MIN) / 2d;
            futPrices.add(last);
        }

        for (int t = time(); t < MAX_TIME; t ++) {
            double price = last + averagePriceDiff(x, t);
            futPrices.add(price);
            last = price;
        }

        return futPrices;
    }

    private List<Double> projectPrices() {
        // project future prices over all x
        List<Double> futPrices = new ArrayList<Double>();
        for (int t = 0; t < MAX_TIME; t ++) {
            futPrices.add(0d);
        }

        for (int x : probX.keySet()) {
            List<Double> pricesX = projectPrices(x);
            // add prediction for this x, weighted by prob of x
            for (int t = 0; t < MAX_TIME; t ++) {
                double weighted = 
                    (double)pricesX.get(t) * (double)probX.get(x);
                futPrices.set(t, futPrices.get(t) + weighted);
            }
        }

        return futPrices;
    }
}
