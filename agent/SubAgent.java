package agent;

import java.util.List;

public abstract class SubAgent<T extends Buyable> {

	protected final Agent agent;
	protected final List<T> stock;

	public SubAgent(Agent agent, List<T> stock) {
		this.agent = agent;
		this.stock = stock;
	}

	// Fullfill the given list of packages
	public void fullfillPackages(List<Package> packages) {
		for (Package pack : packages) {
			fullfillPackage(pack);
		}
	}

	// Buy all components of a particular package
	public abstract void fullfillPackage(Package pack);
}