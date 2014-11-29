package agent;

import java.util.List;

import se.sics.tac.aw.TACAgent;

public class HotelAgent {

	protected List<Package> packages;
	protected boolean[] auctionsClosed = new boolean[10];
	protected int[] ticketsHeld = new int[10];

	public HotelAgent(List<Package> packages, TACAgent agent) {
		this.packages = packages;
		Client c;
		for (Package p : packages) {
			c = p.getClient();
			//TODO migrate this functionality to main Agent
		}
	}

	
	public void quoteUpdated() {
		
	}
	
	public void allQuotesUpdated() {
		
	}
	
	public void updateOffers() {
	}

	public void auctionClosed(int aucID) {
	}

	public void makeBid(int aucID, int[] offers) {
	}
}