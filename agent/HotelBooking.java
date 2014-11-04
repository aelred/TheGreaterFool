package agent;

public class HotelBooking extends Buyable {
	private final boolean towers;

	public HotelBooking(int day, boolean towers) throws IllegalArgumentException {
		super(day);

		// No bookings allowed on last night
		if (day > Agent.NUM_DAYS-2) {
			throw new IllegalArgumentException("Day not within expected range.");
		}
		
		this.towers = towers;
	}
}