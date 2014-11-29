package agent;

public class HotelBooking extends Buyable {
	public final boolean towers;

	public HotelBooking(int day, boolean towers) throws IllegalArgumentException {
		super(day);

		// No bookings allowed on last night
		if (day > Agent.NUM_DAYS-1) {
			throw new IllegalArgumentException("Day not within expected range.");
		}

		this.towers = towers;
	}

	@Override
	public boolean equals(Object obj) {
		return super.equals(obj) && (towers == ((HotelBooking)obj).towers);
	}

	@Override
	public int hashCode() {
		return day * 2 + (towers? 1 : 0);
	}
}
