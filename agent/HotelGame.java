package agent;

public class HotelGame {

	private float[][] askPrices = new float[8][9];
	private int[] closedOn = new int[8];
	
	public void setAskPrice(int day, boolean tt, float price, int elapsedMinutes, boolean closed) {
		Agent.logMessage("hotelGame", "day=" + Integer.toString(day) + ", tt=" + (tt ? "true" : "false")
				+ ", elapsedMinutes=" + Integer.toString(elapsedMinutes));
		int hotelInd = day + (tt ? 4 : 0) - 1;
		int timeInd = elapsedMinutes;
		askPrices[hotelInd][timeInd] = price;
		if (closed) {
			closedOn[hotelInd] = elapsedMinutes;
			for (++timeInd; timeInd < 8; timeInd++) {
				askPrices[hotelInd][timeInd] = price;
			}
		}
	}
	
}
