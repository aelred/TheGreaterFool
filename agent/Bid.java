package agent;

public class Bid {
	private final float price;
	private final int quantity;

	public float getPrice() {
		return price;
	}

	public int getQuantity() {
		return quantity;
	}

	public Bid(float price, int quantity) {
		this.price = price;
		this.quantity = quantity;
	}
}