package agent.hotel;

import java.util.ArrayList;

public class HotelHistory extends ArrayList<HotelGame> {
	
	private static final long serialVersionUID = 1L;

	public float[] averagePriceDifference() {
		if (this.isEmpty())
			return new float[]{25, 25, 25, 25};
		float[] prices = new float[8];
		int gameCount = 0;
		for (HotelGame g : this) {
			gameCount++;
			for (int auction = 0; auction < 8; auction++) {
				prices[auction] += g.getClosePrice(auction);
			}
		}
		for (int auction = 0; auction < 8; auction++) {
			prices[auction] = prices[auction] / gameCount;
		}
		float[] pds = new float[4];
		for (int day = 0; day < 4; day++) {
			pds[day] = prices[day+4] - prices[day];
		}
		return prices;
	}
	
}