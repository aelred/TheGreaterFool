package agent;

import java.util.List;

public abstract class SubAgent<T extends Buyable> {

	protected final Agent agent;
	protected final List<T> stock;

	public SubAgent(Agent agent, List<T> stock) {
		this.agent = agent;
		this.stock = stock;
	}

	// Fulfill the given list of packages
	public abstract void fulfillPackages(List<Package> packages);
}