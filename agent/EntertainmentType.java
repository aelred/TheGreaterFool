package agent;
import se.sics.tac.aw.*;

public enum EntertainmentType {
    ALLIGATOR_WRESTLING(1), AMUSEMENT(2), MUSEUM(3);
    
    private int value;
    public int getValue() { return value; }
    public int getTACPreferenceQueryValue() { return value + 2; }

    private EntertainmentType(int value) {
        this.value = value;
    }
}
