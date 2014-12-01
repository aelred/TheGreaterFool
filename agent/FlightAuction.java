package agent;

import se.sics.tac.aw.TACAgent;

public class FlightAuction extends Auction<FlightTicket> {
	private boolean arrival;

	public FlightAuction(TACAgent agent, int day, boolean arrival) {
		super(agent, day);
		this.arrival = arrival;
	}

    public boolean getArrival() {
        return arrival;
    }

	protected int getAuctionID() {
		return getAuctionID(TACAgent.CAT_FLIGHT, 
			arrival ? TACAgent.TYPE_INFLIGHT : TACAgent.TYPE_OUTFLIGHT);
	}

    protected FlightTicket getBuyable() {
        return new FlightTicket(day, arrival);
    }
}