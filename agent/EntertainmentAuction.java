package agent;

public class EntertainmentAuction extends Auction {
	private EntertainmentType type;

	public EntertainmentAuction(int day, EntertainmentType type) {
		super(day);
		this.type = type;
	}
}