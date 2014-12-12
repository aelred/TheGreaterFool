package agent;

import java.util.HashMap;

public class BidMap extends HashMap<Float,Integer> {
    public static final long serialVersionUID = 1L;

    public BidMap(BidMap workingBids) {
        super(workingBids);
    }

    public BidMap() {
        super();
    }
}
