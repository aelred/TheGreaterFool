package agent;

public abstract class Buyable {
    protected final int day;

    public int getDay() {
        return day;
    }

  	public Buyable(int day) throws IllegalArgumentException {
  		if (day < 0 || day > Agent.NUM_DAYS-1) {
  			throw new IllegalArgumentException("Day not within expected range.");
  		}
  		this.day = day;
  	}
}