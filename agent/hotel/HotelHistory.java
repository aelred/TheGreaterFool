package agent.hotel;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import agent.logging.LogEntry;

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
		return pds;
	}
	
	public static void main(String[] args) {
		HotelHistory hotelHist = new HotelHistory();
		try (InputStream file = new FileInputStream("hotelHistory.hist");
				InputStream buffer = new BufferedInputStream(file);
				ObjectInput input = new ObjectInputStream(buffer);) {
			hotelHist = (HotelHistory) input.readObject();
		} catch (Exception e) {
		}
		SimpleDateFormat f = LogEntry.formatter;
		for (HotelGame g : hotelHist) {
			String dump = "Date = " + f.format(g.start) + "\n";
			dump += "hotelGame\taskPrices:\n";
			dump += "hotelGame\taucID\t0min\t1min\t2min\t3min\t4min\t5min\t6min\t7min\t8min\tclosed on\n";
			for (int aucID = 0; aucID < 8; aucID++) {
				dump += "hotelGame\t" + Integer.toString(aucID) + "\t";
				for (int close = 0; close < 9; close++) {
					dump += Float.toString(g.askPrices[aucID][close]) + "\t";
				}
				dump += Integer.toString(g.closedOn[aucID]);
				dump += "\n";
			}
			System.out.println(dump);
		}
	}
	
}