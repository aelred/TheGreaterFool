package agent;

public class FlightAuction extends Auction {
	private boolean arrival;

	public FlightAuction(int day, boolean arrival) {
		super(day);
		this.arrival = arrival;
	}
}