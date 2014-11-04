package agent;

import java.util.*;

public class Auction {
	private final Buyable item;
	private float askPrice;
	private List<Bid> sellBids = new ArrayList<Bid>();
	private List<Bid> buyBids = new ArrayList<Bid>();

	public Buyable getItem() {
		return item;
	}

	public float getAskPrice() {
		return askPrice;
	}

	public Auction(Buyable item) {
		this.item = item;
	}
}