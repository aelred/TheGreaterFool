package agent;

import se.sics.tac.aw.TACAgent;

public class EntertainmentAuction extends Auction<EntertainmentTicket> {
	private EntertainmentType type;

	public EntertainmentAuction(
			TACAgent agent, int day, EntertainmentType type) {
		super(agent, day);
		this.type = type;
	}

	protected int getAuctionID() {
		return getAuctionID(TACAgent.CAT_ENTERTAINMENT, type.getValue());
	}

    public EntertainmentTicket getBuyable() {
        return new EntertainmentTicket(day, type);
    }
}
