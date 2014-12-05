package agent;

import java.util.List;

public abstract class SubAgent<T extends Buyable> {

	public final Agent agent;
	protected final List<T> stock;

	public SubAgent(Agent agent, List<T> stock) {
		this.agent = agent;
		this.stock = stock;
	}

    public abstract void gameStopped();

    // Stop fulfilling packages and stop bidding if possible
    public abstract void clearPackages();

	// Fulfill the given list of packages
	public abstract void fulfillPackages(List<Package> packages);

    // Return the probability of buying this auction. (1 = certain, 0 = impossible)
    public abstract float purchaseProbability(Auction<?> auction);
}
