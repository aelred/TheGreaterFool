package agent;

import se.sics.tac.aw.TACAgent;

public class HotelAuction extends Auction {
	private boolean towers;

	public HotelAuction(TACAgent agent, int day, boolean towers) {
		super(agent, day);
		this.towers = towers;
	}

	protected int getAuctionID() {
		return getAuctionID(TACAgent.CAT_HOTEL, 
			towers ? TACAgent.TYPE_GOOD_HOTEL : TACAgent.TYPE_CHEAP_HOTEL);
	}
}