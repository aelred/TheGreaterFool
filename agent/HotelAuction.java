package agent;

import agent.Auction.Watcher;
import se.sics.tac.aw.Quote;
import se.sics.tac.aw.TACAgent;

public class HotelAuction extends Auction {
	private boolean towers;
	private int hqw;

	public HotelAuction(TACAgent agent, int day, boolean towers) {
		super(agent, day);
		this.towers = towers;
		hqw = 0;
	}

	protected int getAuctionID() {
		return getAuctionID(TACAgent.CAT_HOTEL, 
			towers ? TACAgent.TYPE_GOOD_HOTEL : TACAgent.TYPE_CHEAP_HOTEL);
	}
	
	@Override
	public void fireQuoteUpdated(Quote quote) {
        hqw = quote.getHQW();
		super.fireQuoteUpdated(quote);
    }
	
	public int getHQW() {
		return hqw;
	}
}