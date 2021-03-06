package agent.hotel;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.List;

import agent.Agent;
import agent.Auction;
import agent.BidInUseException;
import agent.BidMap;
import agent.Buyable;
import agent.Client;
import agent.Package;
import agent.SubAgent;
import agent.logging.AgentLogger;
import agent.logging.Identity;
import se.sics.tac.aw.BidString;
import se.sics.tac.aw.Quote;

public class HotelAgent extends SubAgent<HotelBooking> {

	private static final boolean DEBUG = true;
	private static final double[] bidProportions = { 1.5, 1.1, 1.1, 1.1, 1.2,
			1.3, 1.4, 1.5, 0 };
	private int[] lastUpdateMinute;
	private Auction.Watcher watcher = new Auction.Watcher() {
		AgentLogger aucWatcher = logger.getSublogger("auctionWatcher");

		@Override
		public void auctionQuoteUpdated(Auction<?> auction, Quote quote) {
			HotelAuction a = ((HotelAuction) auction);
			aucWatcher.log("Updating " + auction.toString(), AgentLogger.INFO);
			lastUpdateMinute[hashForIndex(auction.getDay(), a.isTT())]++;
			int lmu = lastUpdateMinute[hashForIndex(auction.getDay(), a.isTT())];
			currentGame.setAskPrice(a.getDay(), a.isTT(), a.getAskPrice(), lmu, false);
			//updateBid(auction.getDay(), ((HotelAuction) auction).isTT());
		}

		@Override
		public void auctionBidUpdated(Auction<?> auction, BidString bidString) {
			aucWatcher.log("Bid updated to " + bidString.getBidString()
					+ " for " + ((HotelAuction) auction).toString(),
					AgentLogger.INFO);
		}

		@Override
		public void auctionBidRejected(Auction<?> auction, BidString bidString) {
			aucWatcher.log("Bid rejected: " + bidString.getBidString()
					+ " for " + ((HotelAuction) auction).toString(),
					AgentLogger.WARNING);
		}

		@Override
		public void auctionBidError(Auction<?> auction, BidString bidString,
				int error) {
			String[] statusName = { "no error", "internal error",
					"agent not auth", "game not found", "not member of game",
					"game future", "game complete", "auction not found",
					"auction closed", "bid not found", "trans not found",
					"cannot withdraw bid", "bad bidstring format",
					"not supported", "game type not supported" };
			aucWatcher.log("Bid error: " + statusName[error] + " on bid: "
					+ bidString.getBidString() + " for "
					+ ((HotelAuction) auction).toString(), AgentLogger.WARNING);
		}

		@Override
		public void auctionBuySuccessful(Auction<?> auction,
				List<Buyable> buyables) {
			aucWatcher.log("Won " + buyables.size() + " rooms in "
					+ (((HotelBooking) buyables.get(0)).towers ? "TT" : "SS")
					+ " for day " + buyables.get(0).getDay()
					+ ". Close price was " + auction.getAskPrice(),
					AgentLogger.INFO);
		}

		@Override
		public void auctionSellSuccessful(Auction<?> auction, int numSold) {
		}

		@Override
		public void auctionClosed(Auction<?> auction) {
			aucWatcher.log("Auction for "
					+ (((HotelAuction) auction).isTT() ? "TT" : "SS")
					+ " on day " + auction.getDay() + " has closed at "
					+ auction.getAskPrice() + " per room", AgentLogger.INFO);
			updateOnAuctionClosed((HotelAuction) auction);
		}
	};

	private boolean[] auctionsClosed;
	private int[] held = new int[8];
	private int[] intentions = new int[8];
	private List<Package> mostRecentPackages;
	private HotelHistory hotelHist;
	private HotelGame currentGame;
	private boolean dirtyHistory = false;
	private AgentLogger pmLogger;

	@SuppressWarnings("unused")
	private boolean[] intendedHotel;

	public HotelAgent(Agent agent, List<HotelBooking> hotelStock,
			AgentLogger logger) {
		super(agent, hotelStock, logger);
		subscribeAll();
		auctionsClosed = new boolean[8];
		for (int day = 1; day < 5; day++) {
			Quote quoteTT = agent.getTACAgent().getQuote(
					getAuctionID(true, day));
			auctionsClosed[hashForIndex(day, true)] = quoteTT.isAuctionClosed();
			Quote quoteSS = agent.getTACAgent().getQuote(
					getAuctionID(false, day));
			auctionsClosed[hashForIndex(day, false)] = quoteSS
					.isAuctionClosed();
		}

		pmLogger = logger.getSublogger("packageManager");

		hotelHist = new HotelHistory();
		try (InputStream file = new FileInputStream("hotelHistory.hist");
				InputStream buffer = new BufferedInputStream(file);
				ObjectInput input = new ObjectInputStream(buffer);) {
			hotelHist = (HotelHistory) input.readObject();
			logger.log("Hotel history loaded with " + hotelHist.getNumGames()
					+ " games");
		} catch (Exception e) {
			logger.log("Unable to read past history file", AgentLogger.WARNING);
			// logger.logExceptionStack(e, AgentLogger.ERROR);
		}
		currentGame = new HotelGame();
		hotelHist.setCurrentGame(currentGame);
		

		if (agent.getTime() > 55000) {
			logger.log("Suspected late start, history marked dirty. Time="
					+ agent.getTime(), AgentLogger.WARNING);
			dirtyHistory = true;
		}

		// Fill held using existing stock of tickets
		for (HotelBooking booking : hotelStock) {
			int i = hashForIndex(booking.getDay(), booking.towers);
			held[i] += 1;
		}
		lastUpdateMinute = new int[] { -1, -1, -1, -1, -1, -1, -1, -1 };
	}

	public void gameStopped() {
		for (int day = 1; day < 5; day++) {
			agent.getHotelAuction(day, true).removeWatcher(watcher);
			agent.getHotelAuction(day, false).removeWatcher(watcher);
		}
		if (!dirtyHistory)
			hotelHist.save();
		else {
			logger.getSublogger("historyRecorder")
					.log("Agent entered game after 55 seconds in, so history will be dirty and will not be saved");
			;
			hotelHist.discard();
		}
		currentGame.dumpToConsole(logger.getSublogger("hotelGame"));

		try (OutputStream file = new FileOutputStream("hotelHistory.hist");
				OutputStream buffer = new BufferedOutputStream(file);
				ObjectOutput output = new ObjectOutputStream(buffer);) {
			output.writeObject(hotelHist);
		} catch (IOException e) {
			logger.log(
					"Warning: unable to write to file hotelHistory.hist. Error message: "
							+ e.getMessage(), AgentLogger.WARNING);
		}
	}

	private void subscribeAll() {
		for (int day = 1; day < 5; day++) {
			agent.getHotelAuction(day, true).addWatcher(watcher);
			agent.getHotelAuction(day, false).addWatcher(watcher);
		}
	}

	private void updateOnAuctionClosed(HotelAuction auction) {
		int day = auction.getDay();
		boolean tt = auction.isTT();
		int i = hashForIndex(day, tt);
		auctionsClosed[i] = true;
		int won = agent.getTACAgent().getOwn(getAuctionID(tt, day));
		held[i] = won;
		for (int a = 1; a <= won; a++) {
			stock.add(new HotelBooking(day, tt));
		}
		currentGame.setAskPrice(day, tt, auction.getAskPrice(),
				lastUpdateMinute[hashForIndex(day, tt)], true);
		hotelHist.update();
		fulfillPackages(mostRecentPackages);
	}

	/**
	 * returns the auction ID for the hotel auction with specified hotel type
	 * and day
	 * 
	 * @param tt
	 *            (boolean) is the auction for Tampa Towers?
	 * @param day
	 *            (int) Monday = 1 to Thursday = 4
	 * @return
	 */
	public int getAuctionID(boolean tt, int day) {
		return 7 + day + (tt ? 4 : 0);
	}

	public void fulfillPackages(List<Package> packages) {
		if (!fulfillPackagesRecursive(packages))
			agent.alertInfeasible();
	}

	private boolean fulfillPackages_(List<Package> packages) {
		// AgentLogger fine = pmLogger.getSublogger("fine");
		boolean[] intendedHotel = new boolean[8];
		int[] allocated = new int[8];
		mostRecentPackages = packages;
		intentions = new int[8];
		boolean tt, err;
		int arrive, depart, hotelPremium, errCount, day, i;
		int[] tempIntentions, tempAllocations;
		Client c;
		int cliNum = 0;
		float[] estDifs = hotelHist.getEstHotelPriceDifs();

		for (Package p : packages) {
			cliNum++;
			pmLogger.log("Sorting preferences for package " + cliNum
					+ " with days " + Integer.toString(p.getArrivalDay())
					+ " to " + Integer.toString(p.getDepartureDay()),
					AgentLogger.INFO);
			c = p.getClient();
			arrive = p.getArrivalDay();
			depart = p.getDepartureDay();
			hotelPremium = c.getHotelPremium();
			// identify if tt is worth aiming for
			int expectedExtra = 0; // expected extra for TT
			for (day = arrive; day < depart; day++) {
				expectedExtra += estDifs[day - 1];
			}
			tt = hotelPremium > expectedExtra;
			pmLogger.log("HP:" + hotelPremium
					+ ", expected difference in cost:" + expectedExtra
					+ ", so getting " + (tt ? "TT" : "SS"));
			errCount = 0;
			do {
				err = false;
				tempIntentions = new int[8];
				tempAllocations = new int[8];
				for (day = arrive; day < depart; day++) {
					i = hashForIndex(day, tt);
					if (!auctionsClosed[i]) {
						// fine.log("Adding temporary intention for " + i);
						tempIntentions[i]++;
					} else {
						if (allocated[i] < held[i]) {
							tempAllocations[i]++;
							// fine.log("Adding temporary allocation for " + i);
						} else {
							// infeasible package, try other hotel
							// fine.log("Hotel infeasible for this time period, temporary intentions and allocations reset");
							errCount++;
							err = true;
							tt = !tt;
							break;
						}
					}
				}
			} while (errCount == 1 && err);
			if (!err) {
				// store hotel intention
				intendedHotel[cliNum - 1] = tt;
				String allocIntent = "\n\t\t0\t1\t2\t3\t4\t5\t6\t7\n", alloc = "allocated", intent = "intentions:";
				// push changes to main intention and allocation arrays
				for (i = 0; i <= 7; i++) {
					allocated[i] = allocated[i] + tempAllocations[i];
					alloc += "\t" + Integer.toString(allocated[i]);
					intentions[i] = intentions[i] + tempIntentions[i];
					intent += "\t" + Integer.toString(intentions[i]);
				}
				pmLogger.log(allocIntent + alloc + "\n" + intent);
			} else {
				// failed to find a feasible solution to this package on
				// specified days
				pmLogger.log("Package " + Integer.toString(cliNum)
						+ " infeasible, requesting package update",
						AgentLogger.WARNING);
				return false;
			}
		}
		// updateBids(); // Bids are updated individually as initial quote
		// updates come in
		// Output intentions
		if (DEBUG) {
			String message = "Intentions:\n";
			cliNum = 0;
			for (Package p : packages) {
				cliNum++;
				message += Integer.toString(cliNum) + ": days "
						+ Integer.toString(p.getArrivalDay()) + " to "
						+ Integer.toString(p.getDepartureDay())
						+ " with premium of "
						+ Integer.toString(p.getClient().getHotelPremium())
						+ ". Attempt to get "
						+ (intendedHotel[cliNum - 1] ? "TT" : "SS") + "\n";
			}
			pmLogger.log(message, AgentLogger.INFO);
		}
		return true;
	}
	
	public boolean fulfillPackagesRecursive(List<Package> packages) {
		numRecs = 0;
		// save packages
		mostRecentPackages = packages;
		// gather arrays
		int len = packages.size(), packageNum = -1;
		int[] startDays = new int[len], endDays = new int[len], hps = new int[len];
		for (Package p : packages) {
			packageNum++;
			startDays[packageNum] = p.getArrivalDay();
			endDays[packageNum] = p.getDepartureDay();
			hps[packageNum] = p.getClient().getHotelPremium();
		}
		float[] predPrices = hotelHist.getEstPrices();
		boolean[] isClosed = auctionsClosed.clone();
		int[] stock = held.clone();
		
		// find best(ish) hotel strategy
		Alloc result = bhsRecurse(0, startDays, endDays, hps, predPrices, isClosed, stock);
		if (!result.feasible) {
			pmLogger.log("Packages infeasible, requesting package update", AgentLogger.WARNING);
			return false;
		}
		String outputStrategy = "";
		for (int p = 0; p < len; p++)
			outputStrategy += result.alloc[p] + " ";
		pmLogger.log("Best hotel strategy found is: " + outputStrategy + ", found after " + numRecs);
		
		// clear previous intentions
		intentions = new int[8];
		// generate intentions from the strategy
		boolean tt;
		int i;
		for (int p = 0; p < len; p++) {
			for (int day = startDays[p]; day < endDays[p]; day++) {
				tt = result.alloc[p];
				i = hashForIndex(day, tt);
				if (isClosed[i]) {
					stock[i]--;
					if (stock[i] < 0)
						pmLogger.log("### VERY BAD THINGS! ###", AgentLogger.ERROR);
				} else
					intentions[i]++;
			}
		}
		String intentionString = "Intentions:";
		String remStockString = "Remaining stock:";
		String predPriceString = "Predicted cost:";
		for (i = 0; i < intentions.length; i++) {
			intentionString += "\t" + (isClosed[i] ? "Closed" : intentions[i]);
			remStockString += "\t" + stock[i] + "/" + held[i];
			predPriceString += "\t" + predPrices[i];
		}
		pmLogger.log(intentionString);
		pmLogger.log(remStockString);
		pmLogger.log(predPriceString);
		updateBids();
		return true;
	}
	
	private int numRecs = 0;
	
	private class Alloc {
		boolean[] alloc;
		float netCost = 0;
		boolean feasible = true;
	}
	
	/**
	 * BestHotelSelectionRecursive
	 * @param depth
	 * @param startDays
	 * @param endDays
	 * @param hps
	 * @param predPrices
	 * @param isClosed
	 * @param stock
	 * @return
	 */
	private Alloc bhsRecurse(int depth, int[] startDays, int[] endDays, int[] hps, float[] predPrices, boolean[] isClosed, int[] stock) {
		numRecs++;
		int startDay = startDays[depth], endDay = endDays[depth], hp = hps[depth];
		int[] ttStock = stock.clone(), ssStock = stock.clone();
		Alloc ttAlloc = new Alloc(), ssAlloc = new Alloc();
		ttAlloc.netCost -= hp;
		// calculate cost for this depth package for both tt and ss possibilities
		int ttDay;
		for (int ssDay = startDay-1; ssDay < endDay-1; ssDay++) {
			ttDay = ssDay + 4;
			if (!isClosed[ttDay])
				ttAlloc.netCost += predPrices[ttDay];
			else if (ttStock[ttDay] > 0)
				ttStock[ttDay]--;
			else
				ttAlloc.feasible = false;
			if (!isClosed[ssDay])
				ssAlloc.netCost += predPrices[ssDay];
			else if (ssStock[ssDay] > 0)
				ssStock[ssDay]--;
			else
				ssAlloc.feasible = false;
		}
		ttAlloc.alloc = new boolean[startDays.length-depth];
		ttAlloc.alloc[0] = true;
		ssAlloc.alloc = new boolean[startDays.length-depth];
		ssAlloc.alloc[0] = false;
		
		// if not at maximum depth, recurse
		if (depth < startDays.length-1) {
			Alloc newTT, newSS;
			newTT = ttAlloc.feasible ? bhsRecurse(depth+1,startDays,endDays,hps,predPrices,isClosed,ttStock) : ttAlloc;
			newSS = ssAlloc.feasible ? bhsRecurse(depth+1,startDays,endDays,hps,predPrices,isClosed,ssStock) : ssAlloc;
			if (!newTT.feasible && !newSS.feasible) {
				return newTT;
			}
			Alloc head, tail;
			if (newTT.feasible && newSS.feasible) {
				ttAlloc.netCost += newTT.netCost;
				ssAlloc.netCost += newSS.netCost;
				if (ttAlloc.netCost < ssAlloc.netCost) {
					head = ttAlloc;
					tail = newTT;
				} else {
					head = ssAlloc;
					tail = newSS;
				}
			} else if (newTT.feasible) {
				ttAlloc.netCost += newTT.netCost;
				head = ttAlloc;
				tail = newTT;
			} else {
				ssAlloc.netCost += newSS.netCost;
				head = ssAlloc;
				tail = newSS;
			}
			for (int i = 0; i < tail.alloc.length; i++)
				head.alloc[i+1] = tail.alloc[i];
			return head;
		} else {
			// at maximum depth (final package) so just return the lowest cost of the two
			if (!ttAlloc.feasible && !ssAlloc.feasible)
				return ttAlloc;
			if (ttAlloc.feasible && ssAlloc.feasible)
				if (ttAlloc.netCost < ssAlloc.netCost)
					return ttAlloc;
				else
					return ssAlloc;
			else if (ttAlloc.feasible)
				return ttAlloc;
			else
				return ssAlloc;
		}
	}
	
	private void updateBids() {
		updateBids(true);
		updateBids(false);
	}

	private void updateBids(boolean tt) {
		for (int day = 1; day < 5; day++) {
			updateBid(day, tt);
		}
	}

	private void updateBid(int day, boolean tt) {
		try {
			int lmu = Math.max(lastUpdateMinute[hashForIndex(day, tt)],0);
			HotelAuction auc = agent.getHotelAuction(day, tt);
			//currentGame.setAskPrice(day, tt, auc.getAskPrice(), lmu, false);
			if (auc.isClosed())
				throw new AuctionClosedException();
			hotelHist.setEstNextPrice(day, tt);
			float estNextPrice = hotelHist.getEstNextPrice(day, tt);
			estNextPrice = Math.max(estNextPrice, auc.getAskPrice() + 75);
			float proposedBid = (float) (estNextPrice * bidProportions[lmu]);
			int dayHash = hashForIndex(day, tt);
			int hqw = auc.getHQW();
			float maxBid = 0;
			if (auc.getActiveBids() != null)
				for (float bid : auc.getActiveBids().keySet())
					maxBid = Math.max(maxBid, bid);
			auc.wipeBid();
			/*
			 * if (auc.getAskPrice() < 1) { auc.modifyBidPoint(16 -
			 * intentions[dayHash] - 1, (float)1.01); }
			 */
			int numIntentions = intentions[dayHash];
			boolean submit = false;
			if (auc.getAskPrice() < 1) {
				auc.modifyBidPoint(8 - numIntentions, (float) 1.01);
				submit = true;
			} else if (numIntentions < hqw) {
				// on target to win surplus to requirement
				auc.modifyBidPoint(hqw - numIntentions, auc.getAskPrice() + 1);
				if (maxBid > auc.getAskPrice() + 1 || auc.getAskPrice() < 3)
					submit = true;
			}
			if (numIntentions > 0) {
				auc.modifyBidPoint(numIntentions, proposedBid);
				submit = true;
			}
			if (submit)
				auc.submitBid(true);
		} catch (AuctionClosedException e) {
			/*logger.log("Attempted to update "
					+ agent.getHotelAuction(day, tt).toString()
					+ " after it had CLOSED", AgentLogger.WARNING);
			*/// e.printStackTrace();
		} catch (BidInUseException e) {
			logger.log("Attempted to update "
					+ agent.getHotelAuction(day, tt).toString()
					+ " while it was BUSY", AgentLogger.WARNING);
			// e.printStackTrace();
		}
	}

	/**
	 * Returns an integer index between 0 and 7. 0 is SS day 1, 3 = SS day 4, TT
	 * is 4 more than the equivalent day of SS
	 * 
	 * @param day
	 *            - integer between 1 and 4
	 * @param tt
	 *            - boolean, is the hotel choice TT?
	 * @return
	 */
	private int hashForIndex(int day, boolean tt) {
		int toReturn = day + (tt ? 4 : 0) - 1;
		if (toReturn < 0 || toReturn > 7)
			logger.log("hashForIndex invalid: " + Integer.toString(toReturn),
					AgentLogger.WARNING);
		return toReturn;
	}

	@Override
	public void clearPackages() {
		intentions = new int[8];
		updateBids();
	}

	public boolean isAuctionClosed(HotelAuction auction) {
		int day = auction.getDay();
		boolean tt = auction.isTT();
		return auctionsClosed[hashForIndex(day, tt)];
	}

	@Override
	public float purchaseProbability(Auction<?> auction) {
		if (auction.getDay()==2 || auction.getDay()==3)
			return (float) 0.9;
		return (float) 0.95;
	}
	
	/*@Override
	public float purchaseProbability(Auction<?> auction) {
		// Assume 8 agents and 8 clients
		int numClients = 8 * 8;

		// Estimate demand
		int day = auction.getDay();
		float probDay = (day == 1 || day == 4) ? 0.4f : 0.6f;
		float demand = (float) numClients * probDay;

		// Work out how many bookings are available to buy
		boolean tt = ((HotelAuction) auction).isTT();
		boolean thisClosed = auctionsClosed[hashForIndex(day, tt)];
		boolean otherClosed = auctionsClosed[hashForIndex(day, !tt)];
		int supply = 0;
		if (!thisClosed) {
			supply += 16;
		} else {
			demand -= 16;
		}
		if (!otherClosed) {
			supply += 16;
		} else {
			demand -= 16;
		}

		// Calculate probability from supply and demand
		float prob;
		if (demand > 0f) {
			prob = (float) supply / demand;
		} else {
			prob = (supply > 0) ? 1f : 0f;
		}
		if (prob > 1f)
			prob = 1f;
		return auctionsClosed[hashForIndex(day, tt)] ? 0 : prob;
	}*/

	@Override
	public float estimatedPrice(Auction<?> auction) {
		int day = auction.getDay();
		boolean tt = ((HotelAuction) auction).isTT();
		float[] estPrices = hotelHist.getEstPrices();
		return estPrices[hashForIndex(day, tt)];
	}

}

class AuctionClosedException extends Exception {
	public static final long serialVersionUID = 1L;
}
