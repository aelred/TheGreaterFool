package agent;

public abstract class Buyable {
    protected final int day;
    protected Package associatedPackage;

    public int getDay() {
        return day;
    }

    public Package getAssociatedPackage() {
        return associatedPackage;
    }
    public void setAssociatedPackage(Package associatedPackage) {
        this.associatedPackage = associatedPackage;
    }

  	public Buyable(int day) throws IllegalArgumentException {
  		if (day < 0 || day > Agent.NUM_DAYS-1) {
  			throw new IllegalArgumentException("Day not within expected range.");
  		}
  		this.day = day;
  	}

    @Override
    public boolean equals(Object obj) {
      return (getClass() == obj.getClass()) && (day == ((Buyable)obj).day);
    }

    @Override
    public int hashCode() {
      return day;
    }
}
