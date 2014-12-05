package agent;

public class HotelGame {

	private float[][] askPrices = new float[8][8];
	private int[] closedOn = new int[8];
	
	public void setAskPrice(int day, boolean tt, float price, int elapsedMinutes, boolean closed) {
		int hotelInd = day + (tt ? 4 : 0) - 1;
		int timeInd = elapsedMinutes - 1;
		askPrices[hotelInd][timeInd] = price;
		for (++timeInd; timeInd < 8 && closed; timeInd++) {
			askPrices[hotelInd][timeInd] = price;
		}
	}
	
}
