package agent;

import java.util.List;

import se.sics.tac.aw.Quote;
import se.sics.tac.aw.TACAgent;

public class HotelAgent {

	protected List<Package> packages;
	protected boolean[] auctionsClosed = new boolean[10];
	protected int[] ticketsHeld = new int[10];
	
	private int[] minimumPremiumPerDay = new int[4];

	public HotelAgent(List<Package> packages, TACAgent agent) {
		this.packages = packages;
		Client c;
		int premiumPerDay, pArrive, pDepart, hotelPremium, auctionID;
		boolean tt;
		for (Package p : packages) {
			c = p.getClient();
			pArrive = c.getPreferredArrivalDay();
			pDepart = c.getPreferredDepartureDay();
			hotelPremium = c.getHotelPremium();
			premiumPerDay = hotelPremium / (pDepart-pArrive);
			tt = premiumPerDay > 35; //TODO this is where the initial hotel choice is decided
			auctionID = getAuctionID(tt,pArrive);
			if (tt) {
				agent.setAllocation(auctionID, agent.getAllocation(auctionID) + 1);
				
			}
			//TODO migrate this functionality to main Agent
		}
	}

	public void clearRequests() {
		
	}
	
	public void addRequest(int beginDay, int endDay, boolean tt, int premiumPerDay) {
		
	}
	
	/**
	 * returns the auction ID for the hotel auction with specified hotel type and day
	 * @param tt (boolean) is the auction for Tampa Towers?
	 * @param day (int) Monday = 1 to Thursday = 4
	 * @return
	 */
	public int getAuctionID(boolean tt, int day) {
		return 7 + day + (tt ? 4 : 0);
	}
	
	public void quoteUpdated(Quote quote) {
		
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