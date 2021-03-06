package agent.hotel;

import agent.Auction;
import se.sics.tac.aw.Quote;
import se.sics.tac.aw.TACAgent;

public class HotelAuction extends Auction<HotelBooking> {
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
	
	public int getAuctionID() {
		return getAuctionID(TACAgent.CAT_HOTEL, 
			tt ? TACAgent.TYPE_GOOD_HOTEL : TACAgent.TYPE_CHEAP_HOTEL);
	}

    public HotelBooking getBuyable() {
        return new HotelBooking(day, tt);
    }
	
	@Override
	public void fireQuoteUpdated(Quote quote) {
        hqw = quote.getHQW();
		super.fireQuoteUpdated(quote);
    }
	
	public int getHQW() {
		if (hqw < 0)
			return 0;
		else
			return hqw;
	}
	
	public boolean isClosed() {
		if (super.getMostRecentQuote() != null)
			return super.getMostRecentQuote().isAuctionClosed();
		else
			return false;
	}
	
	public String toString() {
		return "hotel auction for day=" + Integer.toString(day) + ", hotel=" + (tt ? "TT" : "SS");
	}
}
