package agent.flight;

import se.sics.tac.aw.TACAgent;

import agent.*;

public class FlightAuction extends Auction<FlightTicket> {
	private boolean arrival;

	public FlightAuction(TACAgent agent, int day, boolean arrival) {
		super(agent, day);
		this.arrival = arrival;
	}

    public boolean getArrival() {
        return arrival;
    }

	public int getAuctionID() {
		return getAuctionID(TACAgent.CAT_FLIGHT, 
			arrival ? TACAgent.TYPE_INFLIGHT : TACAgent.TYPE_OUTFLIGHT);
	}

    public FlightTicket getBuyable() {
        return new FlightTicket(day, arrival);
    }
}
