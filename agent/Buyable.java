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
    public void clearAssociatedPackage() {
        this.associatedPackage = null;
    }

  	public Buyable(int day) throws IllegalArgumentException {
  		if (day < 1 || day > Agent.NUM_DAYS) {
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
