package agent.hotel;

import java.util.List;

import agent.Agent;
import agent.Auction;
import agent.BidInUseException;
import agent.Buyable;
import agent.Client;
import agent.Package;
import agent.SubAgent;
import agent.logging.AgentLogger;
import se.sics.tac.aw.BidString;
import se.sics.tac.aw.Quote;

public class HotelAgent extends SubAgent<HotelBooking> {

	private static final boolean DEBUG = true;
	private Auction.Watcher watcher = new Auction.Watcher() {
		AgentLogger aucWatcher = logger.getSublogger("auctionWatcher");
		@Override
		public void auctionQuoteUpdated(Auction<?> auction, Quote quote) {
			aucWatcher.log("Hotel auction updating with day=" + Integer.toString(auction.getDay()),
					AgentLogger.INFO);
			updateBid(auction.getDay(),((HotelAuction)auction).isTT());
		}
		@Override
		public void auctionBidUpdated(Auction<?> auction, BidString bidString) {
			aucWatcher.log("Bid updated to " + bidString.getBidString()
					+ " for " + ((HotelAuction)auction).toString(), AgentLogger.INFO);
		}
		@Override
		public void auctionBidRejected(Auction<?> auction, BidString bidString) {
			aucWatcher.log("Bid rejected: " + bidString.getBidString()
					+ " for " + ((HotelAuction)auction).toString(), AgentLogger.WARNING);
		}
		@Override
		public void auctionBidError(Auction<?> auction, BidString bidString, int error) {
			String[] statusName = {
		            "no error",
		            "internal error",
		            "agent not auth",
		            "game not found",
		            "not member of game",
		            "game future",
		            "game complete",
		            "auction not found",
		            "auction closed",
		            "bid not found",
		            "trans not found",
		            "cannot withdraw bid",
		            "bad bidstring format",
		            "not supported",
		            "game type not supported"
		    };
			aucWatcher.log("Bid error: " + statusName[error] + " on bid: " + bidString.getBidString()
					+ " for " + ((HotelAuction)auction).toString(), AgentLogger.WARNING);
		}
		@Override
		public void auctionTransaction(Auction<?> auction, List<Buyable> buyables) {
			aucWatcher.log("Won " + buyables.size() + " rooms in " + (((HotelBooking)buyables.get(0)).towers ? "TT" : "SS")
				+ " for day " + buyables.get(0).getDay() + ". Close price was " + auction.getAskPrice(), AgentLogger.INFO);
		}
		@Override
		public void auctionClosed(Auction<?> auction) {
			aucWatcher.log("Auction for " + (((HotelAuction)auction).isTT() ? "TT" : "SS") + " on day " 
					+ auction.getDay() + " has closed at " + auction.getAskPrice() + " per room", AgentLogger.INFO);
			updateOnAuctionClosed((HotelAuction) auction);
		}
	};
	
	private boolean[] auctionsClosed;
	private int[] held = new int[8];
	private int[] intentions = new int[8];
	private List<Package> mostRecentPackages;
	private HotelHistory hotelHist;
	private HotelGame currentGame;
	private int numClosed = 0;
	
	@SuppressWarnings("unused")
	private boolean[] intendedHotel;

	public HotelAgent(Agent agent, List<HotelBooking> hotelStock, HotelHistory hh, AgentLogger logger) {
		this(agent,hotelStock, logger);
		hotelHist = hh;
		currentGame = new HotelGame(logger.getSublogger("historyRecorder"));
	}
	
	public HotelAgent(Agent agent, List<HotelBooking> hotelStock, AgentLogger logger) {
		super(agent, hotelStock, logger);
		subscribeAll();
		auctionsClosed = new boolean[8];

		// Fill held using existing stock of tickets
		for (HotelBooking booking : hotelStock) {
		    int i = hashForIndex(booking.getDay(), booking.towers);
		    held[i] += 1;
		}
	}
	
    public void gameStopped() {
    	for (int day = 1; day < 5; day++) {
			agent.getHotelAuction(day, true).removeWatcher(watcher);;
			agent.getHotelAuction(day, false).removeWatcher(watcher);
		}
    	hotelHist.add(currentGame);
    	currentGame.dumpToConsole();
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
		currentGame.setAskPrice(day, tt, auction.getAskPrice(), ++numClosed, true);
		if (held[i] < intentions[i]) {
			// did not achieve ideal scenario, need to reassess
			fulfillPackages(mostRecentPackages);
		}
	}
	
	/**
	 * returns the auction ID for the hotel auction with specified hotel type and day
	 * @param tt (boolean) is the auction for Tampa Towers?
	 * @param day (int) Monday = 1 to Thursday = 4
	 * @return
	 */
	public int getAuctionID(boolean tt, int day) {
		return 7 + day + (tt ? 4 : 0);
	}

	@Override
	public void fulfillPackages(List<Package> packages) {
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
			logger.log("Sorting preferences for package " + cliNum + " with days " 
					+ Integer.toString(p.getArrivalDay()) + " to " + Integer.toString(p.getDepartureDay()), AgentLogger.INFO);
			c = p.getClient();
			arrive = p.getArrivalDay();
			depart = p.getDepartureDay();
			hotelPremium = c.getHotelPremium();
			//tt = hotelPremium / (depart - arrive) > 35; // identify if tt is worth aiming for
			int expectedExtra = 0; // expected extra for TT
			for (day = arrive; day < depart; day++) {
				expectedExtra += avgDifs[day-1];
			}
			tt = hotelPremium > expectedExtra;
			errCount = 0;
			do {
				err = false;
				tempIntentions = new int[8];
				tempAllocations = new int[8];
				for (day = arrive; day < depart; day++) {
					i = hashForIndex(day, tt);
					if (!auctionsClosed[i]) {
						tempIntentions[i]++;
					} else {
						if (allocated[i] < held[i]) {
							tempAllocations[i]++;
						} else {
							// infeasible package, try other hotel
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
				intendedHotel[cliNum-1] = tt;
				// push changes to main intention and allocation arrays
				for (i = 0; i <= 7; i++) {
					allocated[i] = allocated[i] + tempAllocations[i];
					intentions[i] = intentions[i] + tempIntentions[i];
				}
			} else {
				// failed to find a feasible solution to this package on specified days
				logger.log("Package " + Integer.toString(cliNum) + " infeasible, requesting package update",
						AgentLogger.WARNING);
				agent.alertInfeasible();
			}
		}
		//updateBids(); // Bids are updated individually as initial quote updates come in
		// Output intentions
		if (DEBUG) {
			String message = "Intentions:\n";
			cliNum = 0;
			for (Package p : packages) {
				cliNum++;
				message += Integer.toString(cliNum) + ": days " + Integer.toString(p.getArrivalDay())
						+ " to " + Integer.toString(p.getDepartureDay()) + " with premium of "
						+ Integer.toString(p.getClient().getHotelPremium()) + ". Attempt to get "
						+ (intendedHotel[cliNum-1] ? "TT" : "SS") + "\n";
			}
			logger.log(message, AgentLogger.INFO);
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
			currentGame.setAskPrice(day, tt, auc.getAskPrice(), numClosed, false);
			int dayHash = hashForIndex(day, tt);
			if (auc.isClosed())
				throw new AuctionClosedException();
			int hqw = auc.getHQW();
			auc.wipeBid();
			/*if (auc.getAskPrice() < 1) {
				auc.modifyBidPoint(16 - intentions[dayHash] - 1, (float)1.01);
			}*/
			if (intentions[dayHash] < hqw) { // on target to win surplus to requirement
				auc.modifyBidPoint(hqw - intentions[dayHash], auc.getAskPrice() + 1);
			} else if (auc.getAskPrice() < 1) {
				auc.modifyBidPoint(intentions[hashForIndex(day, !tt)], (float)1.01);
			}
			auc.modifyBidPoint(intentions[dayHash], auc.getAskPrice() + 50);
			auc.submitBid(true);
		} catch (AuctionClosedException e) {
			logger.log("Attempted to update " + agent.getHotelAuction(day, tt).toString() + 
					" after it had CLOSED", AgentLogger.WARNING);
			//e.printStackTrace();
		} catch (BidInUseException e) {
			logger.log("Attempted to update " + agent.getHotelAuction(day, tt).toString() + 
					" while it was BUSY", AgentLogger.WARNING);
			//e.printStackTrace();
		}	
	}
	
	/**
	 * Returns an integer index between 0 and 7. 0 is SS day 1, 3 = SS day 4,
	 * TT is 4 more than the equivalent day of SS
	 * @param day - integer between 1 and 4
	 * @param tt - boolean, is the hotel choice TT?
	 * @return
	 */
	private int hashForIndex(int day, boolean tt) {
		int toReturn = day + (tt ? 4 : 0) - 1;
		if (toReturn < 0 || toReturn > 7)
			logger.log("hashForIndex invalid: " + Integer.toString(toReturn), AgentLogger.WARNING);
		return toReturn;
	}

	
	@Override
	public void clearPackages() {
		intentions = new int[8];
		updateBids();
	}

	@Override
	public float purchaseProbability(Auction<?> auction) {
		//TODO make this actually probabilistic
		int day = auction.getDay();
		boolean tt = ((HotelAuction)auction).isTT();
		return auctionsClosed[hashForIndex(day, tt)] ? 0 : 1;
	}

	@Override
	public float estimatedPrice(Auction<?> auction) {
	    //TODO implement
	    return 50f;
	}

}

class AuctionClosedException extends Exception {
    public static final long serialVersionUID = 1L;
}


