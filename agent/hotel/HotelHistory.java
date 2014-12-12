package agent.hotel;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import agent.logging.AgentLogger;
import agent.logging.LogEntry;

public class HotelHistory implements Serializable {

	private static final long serialVersionUID = 1L;

	private ArrayList<HotelGame> history;
	private HotelGame currentGame;

	private float[] avgHotelPriceDifs;
	private float[][] avgPriceRises;
	private float[][] avgPrices;
	private float[] estHotelPriceDifs;
	private float[] estPrices;
	private float[] estNextPrices = new float[8];
	private AgentLogger logger;

	public void setLogger(AgentLogger logger) {
		this.logger = logger;
	}
	
	public HotelHistory() {
		history = new ArrayList<HotelGame>();
		setAvgHotelPriceDifs();
		setAvgPriceRises();
		setAvgPrices();
	}

	public void update() {
		setEstPrices();
		setEstHotelPriceDifs();
	}

	public void discard() {
		currentGame = null;
	}

	public void save() {
		history.add(currentGame);
		setAvgHotelPriceDifs();
		setAvgPriceRises();
		setAvgPrices();
		currentGame = null;
	}

	public static void main(String[] args) {
		HotelHistory hotelHist = new HotelHistory();
		try (InputStream file = new FileInputStream("hotelHistory.hist");
				InputStream buffer = new BufferedInputStream(file);
				ObjectInput input = new ObjectInputStream(buffer);) {
			hotelHist = (HotelHistory) input.readObject();
		} catch (Exception e) {
		}
		if (args.length == 0) {
			
			SimpleDateFormat f = LogEntry.formatter;
			for (HotelGame g : hotelHist.history) {
				String dump = "Date = " + f.format(g.start) + "\n";
				dump += "hotelGame\taskPrices:\n";
				dump += "hotelGame\taucID\t0min\t1min\t2min\t3min\t4min\t5min\t6min\t7min\t8min\tclosed on\n";
				for (int aucID = 0; aucID < 8; aucID++) {
					dump += "hotelGame\t" + Integer.toString(aucID) + "\t";
					for (int close = 0; close < 9; close++) {
						dump += Float.toString(g.askPrices[aucID][close])
								+ "\t";
					}
					dump += Integer.toString(g.closedOn[aucID]);
					dump += "\n";
				}
				System.out.println(dump);
			}
			System.out.println("Average prices");
			System.out.println("1\t2\t3\t4\t5\t6\t7\t8");
			System.out.println();
			java.text.DecimalFormat form = new java.text.DecimalFormat("#.##");
			for (int aucID = 0; aucID < 8; aucID++) {
				for (int minute = 1; minute <= 8; minute++) {
					System.out.print(form
							.format(hotelHist.avgPrices[aucID][minute - 1])
							+ "\t");
				}
				System.out.println();
			}
		} else {
			
		}
	}

	public float[] getAvgHotelPriceDifs() {
		return avgHotelPriceDifs;
	}

	private void setAvgHotelPriceDifs() {
		if (history.isEmpty()) {
			avgHotelPriceDifs = new float[] { 25, 25, 25, 25 };
			return;
		}
		float[] prices = new float[8];
		int gameCount = 0;
		for (HotelGame g : history) {
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
			pds[day] = prices[day + 4] - prices[day];
		}
		avgHotelPriceDifs = pds;
	}

	public float[][] getAvgPriceRises() {
		return avgPriceRises;
	}

	private void setAvgPriceRises() {
		if (history.isEmpty()) {
			float[] tt = new float[] { 125, 75, 25, 25, 25, 25, 25, 25 };
			float[] ss = new float[] { 100, 50, 15, 15, 15, 15, 15, 15 };
			avgPriceRises = new float[][] { ss, ss, ss, ss, tt, tt, tt, tt };
			return;
		}
		float[][] newAPR = new float[8][8];
		int[][] numInstances = new int[8][8];
		for (HotelGame g : history) {
			float[][] prices = g.askPrices;
			int[] closedOn = g.closedOn;
			for (int aucID = 0; aucID < 8; aucID++) {
				for (int minute = 1; minute <= closedOn[aucID]; minute++) {
					newAPR[aucID][minute - 1] += prices[aucID][minute]
							- prices[aucID][minute - 1];
					numInstances[aucID][minute - 1]++;
				}
			}
		}
		for (int aucID = 0; aucID < 8; aucID++) {
			for (int minute = 1; minute <= 8; minute++) {
				if (numInstances[aucID][minute - 1] == 0)
					newAPR[aucID][minute - 1] = 0;
				else
					newAPR[aucID][minute - 1] = newAPR[aucID][minute - 1]
							/ numInstances[aucID][minute - 1];
			}
		}
		avgPriceRises = newAPR;
	}

	public float[][] getAvgPrices() {
		return avgPrices;
	}

	private void setAvgPrices() {
		float[][] newAP = new float[8][8];
		float count;
		for (int aucID = 0; aucID < 8; aucID++) {
			count = 0;
			for (int minute = 1; minute <= 8; minute++) {
				count += avgPriceRises[aucID][minute - 1];
				newAP[aucID][minute - 1] = count;
			}
		}
		avgPrices = newAP;
	}

	public float[] getEstHotelPriceDifs() {
		return estHotelPriceDifs;
	}

	private void setEstHotelPriceDifs() {
		float[] newEHPD = new float[4];
		for (int day = 0; day < 4; day++) {
			newEHPD[day] = estPrices[day + 4] - estPrices[day];
		}
		estHotelPriceDifs = newEHPD;
	}

	public HotelGame getCurrentGame() {
		return currentGame;
	}

	public void setCurrentGame(HotelGame currentGame) {
		this.currentGame = currentGame;
		update();
	}

	public float[] getEstPrices() {
		return estPrices;
	}

	private void setEstPrices() {
		float currentPrice, avgPrice, avgClosePrice;
		int numSamples, mostRecentMinute = currentGame.mostRecentInfo;
		float[] newEP = new float[8];
		for (int aucID = 0; aucID < 8; aucID++) {
			if (mostRecentMinute > 0) {
				currentPrice = currentGame.askPrices[aucID][mostRecentMinute];
				if (currentGame.closedOn[aucID] != 0) {
					newEP[aucID] = currentPrice;
				} else {
					avgPrice = avgPrices[aucID][mostRecentMinute - 1];
					numSamples = 0;
					avgClosePrice = 0;
					for (int minute = mostRecentMinute; minute < 8; minute++) {
						avgClosePrice += avgPrices[aucID][minute];
						numSamples++;
					}
					avgClosePrice = avgClosePrice / numSamples;
					logger.log(avgPrice + "\t" + avgClosePrice + "\t" + numSamples + "\t" + currentPrice);
					if (avgPrice == 0) {
						float pricePerMinute = currentPrice / mostRecentMinute;
						double avgMinRemaining = (8.0 - mostRecentMinute) / 2;
						newEP[aucID] = (float) (pricePerMinute * (mostRecentMinute + avgMinRemaining));
					} else
						newEP[aucID] = avgClosePrice * currentPrice / avgPrice;
				}
			} else { // mostRecentMinute == 0
				avgPrice = 0;
				for (int minute = 1; minute <= 8; minute++) {
					avgPrice += avgPrices[aucID][minute - 1];
				}
				avgPrice = avgPrice / 8;
				newEP[aucID] = avgPrice;
			}
		}
		estPrices = newEP;
	}

	public int getNumGames() {
		return history.size();
	}

	public float getEstNextPrice(int day, boolean tt) {
		return estNextPrices[day - 1 + (tt ? 4 : 0)];
	}

	public void setEstNextPrice(int day, boolean tt) {
		int aucID = day - 1 + (tt ? 4 : 0);
		if (currentGame.closedOn[aucID] > 0) {
			estNextPrices[aucID] = currentGame.askPrices[aucID][currentGame.closedOn[aucID]];
		} else if (currentGame.mostRecentInfo > 0) {
			float currentAskPrice = currentGame.askPrices[aucID][currentGame.mostRecentInfo];
			float avgAskPrice = avgPrices[aucID][currentGame.mostRecentInfo - 1];
			float avgNextPrice = avgPrices[aucID][currentGame.mostRecentInfo];
			if (avgAskPrice == 0) {
				int mostRecentMinute = currentGame.mostRecentInfo;
				float pricePerMinute = currentAskPrice / mostRecentMinute;
				double avgMinRemaining = (8.0 - mostRecentMinute) / 2;
				estNextPrices[aucID] = (float) (pricePerMinute * (mostRecentMinute + avgMinRemaining));
			} else
				estNextPrices[aucID] = avgNextPrice * currentAskPrice
						/ avgAskPrice;
		} else
			estNextPrices[aucID] = avgPrices[aucID][0];
	}

}