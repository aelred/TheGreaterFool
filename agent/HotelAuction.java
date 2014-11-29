package agent;

public class HotelAuction extends Auction {
	private boolean towers;

	public HotelAuction(int day, boolean towers) {
		super(day);
		this.towers = towers;
	}
}