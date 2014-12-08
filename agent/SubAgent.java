package agent;

import java.util.List;

import agent.logging.AgentLogger;

public abstract class SubAgent<T extends Buyable> {

	public final Agent agent;
	protected final List<T> stock;
	protected final AgentLogger logger;

	public SubAgent(Agent agent, List<T> stock, AgentLogger logger) {
		this.agent = agent;
		this.stock = stock;
		this.logger = logger;
	}

    public abstract void gameStopped();

    // Stop fulfilling packages and stop bidding if possible
    public abstract void clearPackages();

	// Fulfill the given list of packages
	public abstract void fulfillPackages(List<Package> packages);

    // Return the probability of buying this auction. (1 = certain, 0 = impossible)
    public abstract float purchaseProbability(Auction<?> auction);

    // Return the estimated price for one unit in the specified auction.
    // If purchase is impossible, return 0.
    public abstract float estimatedPrice(Auction<?> auction);
}
