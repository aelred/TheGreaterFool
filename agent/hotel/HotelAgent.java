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
	private int[] lastUpdateMinute = new int[] { -1, -1, -1, -1, -1, -1, -1, -1 };
	private Auction.Watcher watcher = new Auction.Watcher() {
		AgentLogger aucWatcher = logger.getSublogger("auctionWatcher");

		@Override
		public void auctionQuoteUpdated(Auction<?> auction, Quote quote) {
			aucWatcher.log("Updating " + ((HotelAuction) auction).toString(),
					AgentLogger.INFO);
			lastUpdateMinute[hashForIndex(auction.getDay(),
					((HotelAuction) auction).isTT())]++;
			updateBid(auction.getDay(), ((HotelAuction) auction).isTT());
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
        public void auctionBuySuccessful(Auction<?> auction, List<Buyable> buyables) {
			aucWatcher.log("Won " + buyables.size() + " rooms in "
					+ (((HotelBooking) buyables.get(0)).towers ? "TT" : "SS")
					+ " for day " + buyables.get(0).getDay()
					+ ". Close price was " + auction.getAskPrice(),
					AgentLogger.INFO);
		}

        @Override
        public void auctionSellSuccessful(Auction<?> auction, int numSold) { }
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
		
		pmLogger = logger.getSublogger("packageManager");

		hotelHist = new HotelHistory();
		try (InputStream file = new FileInputStream("hotelHistory.hist");
				InputStream buffer = new BufferedInputStream(file);
				ObjectInput input = new ObjectInputStream(buffer);) {
			hotelHist = (HotelHistory) input.readObject();
		} catch (Exception e) {
			logger.log("Unable to read past history file", AgentLogger.ERROR);
			logger.logExceptionStack(e, AgentLogger.ERROR);
		}
		currentGame = new HotelGame(logger.getSublogger("historyRecorder"));

		if (agent.getTime() > 55000) {
			logger.log("Suspected late start, history marked dirty. Time=" + agent.getTime(), AgentLogger.WARNING);
			dirtyHistory = true;
		}

		// Fill held using existing stock of tickets
		for (HotelBooking booking : hotelStock) {
			int i = hashForIndex(booking.getDay(), booking.towers);
			held[i] += 1;
		}
	}

	public void gameStopped() {
		for (int day = 1; day < 5; day++) {
			agent.getHotelAuction(day, true).removeWatcher(watcher);
			agent.getHotelAuction(day, false).removeWatcher(watcher);
		}
		if (!dirtyHistory)
			hotelHist.add(currentGame);
		else
			logger.getSublogger("historyRecorder").log("Agent entered game after 55 seconds in, so history will be dirty and will not be saved");;
		currentGame.dumpToConsole();

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
		if (held[i] < intentions[i]) {
			// did not achieve ideal scenario, need to reassess
			fulfillPackages(mostRecentPackages);
		} else if (held[i] > intentions[i]) {
			// TODO may want to reevaluate here to see if money can be saved by
			// using the spare bookings
		}
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

	@Override
	public void fulfillPackages(List<Package> packages) {
		AgentLogger fine = pmLogger.getSublogger("fine");
		boolean[] intendedHotel = new boolean[8];
		int[] allocated = new int[8];
		mostRecentPackages = packages;
		intentions = new int[8];
		boolean tt, err;
		int arrive, depart, hotelPremium, errCount, day, i;
		int[] tempIntentions, tempAllocations;
		Client c;
		int cliNum = 0;
		float[] avgDifs = hotelHist.averagePriceDifference();
		
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
			// tt = hotelPremium / (depart - arrive) > 35; // identify if tt is
			// worth aiming for
			int expectedExtra = 0; // expected extra for TT
			for (day = arrive; day < depart; day++) {
				expectedExtra += avgDifs[day - 1];
			}
			tt = hotelPremium > expectedExtra;
			pmLogger.log("HP:" + hotelPremium + ", expected difference in cost:" + expectedExtra + ", so getting " + (tt?"TT":"SS"));
			errCount = 0;
			do {
				err = false;
				tempIntentions = new int[8];
				tempAllocations = new int[8];
				for (day = arrive; day < depart; day++) {
					i = hashForIndex(day, tt);
					if (!auctionsClosed[i]) {
						fine.log("Adding temporary intention for " + i);
						tempIntentions[i]++;
					} else {
						if (allocated[i] < held[i]) {
							tempAllocations[i]++;
							fine.log("Adding temporary allocation for " + i);
						} else {
							// infeasible package, try other hotel
							fine.log("Hotel infeasible for this time period, temporary intentions and allocations reset");
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
				agent.alertInfeasible();
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
			HotelAuction auc = agent.getHotelAuction(day, tt);
			currentGame.setAskPrice(day, tt, auc.getAskPrice(),
					lastUpdateMinute[hashForIndex(day, tt)], false);
			int dayHash = hashForIndex(day, tt);
			if (auc.isClosed())
				throw new AuctionClosedException();
			int hqw = auc.getHQW();
			auc.wipeBid();
			/*
			 * if (auc.getAskPrice() < 1) { auc.modifyBidPoint(16 -
			 * intentions[dayHash] - 1, (float)1.01); }
			 */
			if (intentions[dayHash] < hqw) { // on target to win surplus to
												// requirement
				auc.modifyBidPoint(hqw - intentions[dayHash],
						auc.getAskPrice() + 1);
			} else if (auc.getAskPrice() < 1) {
				auc.modifyBidPoint(intentions[hashForIndex(day, !tt)],
						(float) 1.01);
			}
			auc.modifyBidPoint(intentions[dayHash], auc.getAskPrice() + 50);
			auc.submitBid(true);
		} catch (AuctionClosedException e) {
			logger.log("Attempted to update "
					+ agent.getHotelAuction(day, tt).toString()
					+ " after it had CLOSED", AgentLogger.WARNING);
			// e.printStackTrace();
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

	@Override
	public float purchaseProbability(Auction<?> auction) {
		// TODO make this actually probabilistic
		int day = auction.getDay();
		boolean tt = ((HotelAuction) auction).isTT();
		return auctionsClosed[hashForIndex(day, tt)] ? 0 : 1;
	}

	@Override
	public float estimatedPrice(Auction<?> auction) {
		// TODO implement
		return 50f;
	}

}

class AuctionClosedException extends Exception {
	public static final long serialVersionUID = 1L;
}
