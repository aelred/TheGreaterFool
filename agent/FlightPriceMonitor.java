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

    // The plane ticket auction to monitor
    private final FlightAuction auction;

    // Historic prices
    private final double[] prices = new double[MAX_TIME];

    // Estimates of the X constant affecting prices
    private final Map<Integer, Double> probX = new HashMap<Integer, Double>();

    public FlightPriceMonitor(FlightAuction auction) {
        this.auction = auction;

        // Set initial probabilities using probability density
        double initX = 1d / (double)(X_MAX - X_MIN + 1);

        for (int x = X_MIN; x <= X_MAX; x ++) {
            probX.put(x, initX);
        }

        // Set prices to default value (-1)
        for (int t = 0; t < MAX_TIME; t++) {
            prices[t] = -1;
        }
    }

    public FlightAuction getAuction() {
        return auction;
    }

    // Time should be an integer measured in 10 second intervals, from 0
    public void addQuote(double quote, int time) throws IllegalArgumentException {
        // Test quote is within expected range
        double min = PRICE_MIN;
        double max = PRICE_MAX;
        if (time == 0) {
            min = START_MIN;
            max = START_MAX;
        }

        if (quote < min || quote > max) {
            throw new IllegalArgumentException(
                "Price quote outside expected range: " + quote);
        }

        prices[time] = quote;

        // Take change in prices to update estimate
        if (time != 0 && prices[time-1] != -1) {
            updateEstimate(prices[time] - prices[time-1], time);
        }
    }

    public int predictMinimumTime(int time) {
        return findMinTime(projectPrices(time));
    }

    public double predictMinimumPrice(int time) {
        return findMinPrice(projectPrices(time));
    }

    public List<Double> priceCumulativeDist(int time) {
        // Return a cumulative distribution of prices.
        // dist.get(p) gives the probability the actual price will be less than p

        List<Double> dist = new ArrayList<Double>();

        final Map<Integer, Double> mins = new HashMap<Integer, Double>();
        List<Integer> xByPrice = new ArrayList<Integer>();

        // Find all predicted minimum prices
        for (int x = X_MIN; x <= X_MAX; x ++) {
            xByPrice.add(x);
            mins.put(x, findMinPrice(projectPrices(x, time)));
        }

        // Sort xByPrice to be sorted by price as promised!
        Collections.sort(xByPrice, new Comparator<Integer>() {
            @Override
            public int compare(Integer x1, Integer x2) {
                return mins.get(x1).compareTo(mins.get(x2));
            }
        });

        Iterator<Integer> xs = xByPrice.iterator();
        int x = xs.next();
        boolean isX = true;
        double cumulative = 0d;

        for (int price = 0; price < PRICE_MAX; price ++) {
            // when we encounter a possible minimum, increase cumulative distribution
            if (isX && price >= mins.get(x)) {
                // increase chance of this price by probability of X
                cumulative += probX.get(x);

                // look at next minimum price
                if (xs.hasNext()) {
                    x = xs.next();
                } else {
                    isX = false;
                }
            }

            dist.add(cumulative);
        }

        return dist;
    }

    private double findMinPrice(double[] prices) {
        double min = PRICE_MAX;
        for (int i = 0; i < prices.length; i ++) {
            if (prices[i] < min && prices[i] != -1) {
                min = prices[i];
            }
        }
        return min;
    }

    private int findMinTime(double[] prices) {
        double min = PRICE_MAX;
        int time = 0;
        for (int i = 0; i < prices.length; i ++) {
            if (prices[i] < min && prices[i] != -1) {
                min = prices[i];
                time = i;
            }
        }
        return time;
    }

    private double xFunc(int x, int t) {
        return 10d + ((double)t - (double)MAX_TIME) * ((double)x - 10d);
    }

    private double probDiffGivenX(double diff, int x, int t) {
        double f = xFunc(x, t);

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

    private void updateEstimate(double diff, int t) {
        // Use Baye's theorem to improve estimates

        // Find probability of quote occuring for every x
        Map<Integer, Double> probDiff = new HashMap<Integer, Double>();
        double probDiffAll = 0d;

        for (int x = X_MIN; x <= X_MAX; x ++) {
            double prob = probDiffGivenX(diff, x, t) * probX.get(x);
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

    private double[] projectPrices(int x, int t) {
        // project future price from t given x
        double[] futPrices = new double[MAX_TIME];
        for (int i = 0; i <= t; i ++) {
            futPrices[i] = prices[i];
        }

        double last;
        if (t != 0) {
            last = futPrices[t];
        } else {
            // Predict initial price
            last = (START_MAX - START_MIN) / 2d;
            futPrices[0] = last;
        }

        for (int i = t + 1; i < MAX_TIME; i ++) {
            double price = last + averagePriceDiff(x, i);
            futPrices[i] = price;
            last = price;
        }

        return futPrices;
    }

    private double[] projectPrices(int t) {
        // project future prices over all x
        double[] futPrices = new double[MAX_TIME];
        for (int i = 0; i < MAX_TIME; i ++) {
            futPrices[i] = 0d;
        }

        for (int x : probX.keySet()) {
            double[] pricesX = projectPrices(x, t);
            // add prediction for this x, weighted by prob of x
            for (int i = 0; i < MAX_TIME; i ++) {
                if (pricesX[i] != -1) {
                    double weighted = pricesX[i] * probX.get(x);
                    futPrices[i] += weighted;
                } else {
                    futPrices[i] = -1;
                }
            }
        }

        return futPrices;
    }
}
