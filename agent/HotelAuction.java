package agent;

import se.sics.tac.aw.Quote;
import se.sics.tac.aw.TACAgent;

public class HotelAuction extends Auction {
	private boolean tt;
	private int hqw;

	public HotelAuction(TACAgent agent, int day, boolean tt) {
		super(agent, day);
		this.tt = tt;
		hqw = 0;
	}

	public boolean isTT() {
		return tt;
	}
	
	protected int getAuctionID() {
		return getAuctionID(TACAgent.CAT_HOTEL, 
			tt ? TACAgent.TYPE_GOOD_HOTEL : TACAgent.TYPE_CHEAP_HOTEL);
	}
	
	@Override
	public void fireQuoteUpdated(Quote quote) {
        hqw = quote.getHQW();
		super.fireQuoteUpdated(quote);
    }
	
	public int getHQW() {
		return hqw;
	}
	
	public boolean isClosed() {
		return super.getMostRecentQuote().isAuctionClosed();
	}
	
	public String toString() {
		return "hotel auction for day=" + Integer.toString(day) + ", hotel=" + (tt ? "TT" : "SS");
	}
}