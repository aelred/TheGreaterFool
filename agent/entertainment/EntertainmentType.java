package agent.entertainment;
import se.sics.tac.aw.*;

import java.util.Random;

public enum EntertainmentType {
    ALLIGATOR_WRESTLING(1), AMUSEMENT(2), MUSEUM(3);
    
    private final int value;
    public int getValue() { return value; }
    public int getTACPreferenceQueryValue() { return value + 2; }

    private EntertainmentType(int value) {
        this.value = value;
    }

    public static EntertainmentType fromValue(int value) {
        switch (value) {
            case 1: return ALLIGATOR_WRESTLING;
            case 2: return AMUSEMENT;
            case 3: return MUSEUM;
            default: throw new AssertionError("Value not in range.");
        }
    }

    public static EntertainmentType randomType(Random rnd) {
        return fromValue(rnd.nextInt(3) + 1);
    }

    public static EntertainmentType randomType(Random rnd, EntertainmentType exclude) {
        EntertainmentType c0, c1;
        switch (exclude) {
            default:
            case ALLIGATOR_WRESTLING:
                c0 = AMUSEMENT; c1 = MUSEUM;
                break;
            case AMUSEMENT:
                c0 = ALLIGATOR_WRESTLING; c1 = MUSEUM;
                break;
            case MUSEUM:
                c0 = ALLIGATOR_WRESTLING; c1 = AMUSEMENT;
                break;
        }
        return (rnd.nextInt(2) == 0) ? c0: c1;
    }
}
