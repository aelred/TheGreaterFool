package agent.hotel;

import agent.Agent;

public class HotelGame {

	private float[][] askPrices = new float[8][9];
	private int[] closedOn = new int[8];
	
	public void setAskPrice(int day, boolean tt, float price, int elapsedMinutes, boolean closed) {
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
	
	public float getClosePrice(int aucID) {
		return askPrices[aucID][closedOn[aucID]];
	}

	public void dumpToConsole() {
		String dump = "\n";
		dump += "hotelGame\taskPrices:\n";
		dump += "hotelGame\taucID\t0min\t1min\t2min\t3min\t4min\t5min\t6min\t7min\t8min\tclosed on\n";
		for (int aucID = 0; aucID < 8; aucID++) {
			dump += "hotelGame\t" + Integer.toString(aucID) + "\t";
			for (int close = 0; close < 9; close++) {
				dump += Float.toString(askPrices[aucID][close]) + "\t";
			}
			dump += Integer.toString(closedOn[aucID]);
			dump += "\n";
		}
		Agent.logMessage("hotelGame", dump);
	}
	
}
