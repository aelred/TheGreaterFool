package agent;

import java.util.List;

public class HotelAgent {

	protected List<Package> packages;
	protected boolean[] auctionsClosed = new boolean[10];
	protected int[] ticketsHeld = new int[10];

	public HotelAgent(List<Package> packages) {
		this.packages = packages;
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