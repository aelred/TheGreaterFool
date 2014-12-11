package agent.hotel;

import java.io.Serializable;
import java.util.Date;

import agent.logging.AgentLogger;

public class HotelGame implements Serializable {

	private static final long serialVersionUID = -7304525179776746139L;
	protected float[][] askPrices = new float[8][9];
	protected int[] closedOn = new int[8];
	protected Date start = new Date();
	protected int mostRecentInfo = 0;
	private static AgentLogger histLogger;
	
	public HotelGame(AgentLogger l) {
		histLogger = l;
	}
	
	public void setAskPrice(int day, boolean tt, float price, int elapsedMinutes, boolean closed) {
		int hotelInd = day + (tt ? 4 : 0) - 1;
		int timeInd = elapsedMinutes;
		askPrices[hotelInd][timeInd] = price;
		if (closed) {
			closedOn[hotelInd] = elapsedMinutes;
			for (++timeInd; timeInd <= 8; timeInd++) {
				askPrices[hotelInd][timeInd] = price;
			}
		}
		mostRecentInfo = elapsedMinutes;
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
		histLogger.log(dump, AgentLogger.INFO);
	}
	
}
