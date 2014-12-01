package agent;

import java.util.List;
import java.util.Map;

import agent.Auction.Watcher;

import se.sics.tac.aw.BidString;
import se.sics.tac.aw.Quote;
import se.sics.tac.aw.TACAgent;
import se.sics.tac.aw.Transaction;

public class HotelAgent extends SubAgent<HotelBooking> {

	private boolean[] auctionsClosed = new boolean[8];
	private int[] held = new int[8];
	private int[] intentions = new int[8];
	private List<Package> mostRecentPackages;

	public HotelAgent(Agent agent, List<HotelBooking> hotelStock) {
		super(agent, hotelStock);
		Auction.Watcher watcher = new Auction.Watcher() {
			@Override
			public void auctionQuoteUpdated(Auction auction, Quote quote) {
					updateBid(auction.getDay(),((HotelAuction)auction).isTT());
			}
			@Override
			public void auctionBidUpdated(Auction auction, BidString bidString) {
				Agent.log.fine("Bid updated to " + bidString.getBidString()
						+ " for " + ((HotelAuction)auction).toString());
			}
			@Override
			public void auctionBidRejected(Auction auction, BidString bidString) {
				Agent.log.fine("Bid rejected: " + bidString.getBidString()
						+ " for " + ((HotelAuction)auction).toString());
			}
			@Override
			public void auctionBidError(Auction auction, BidString bidString, int error) {
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
				Agent.log.fine("Bid error: " + statusName[error] + " on bid: " + bidString.getBidString()
						+ " for " + ((HotelAuction)auction).toString());
			}
			@Override
			public void auctionTransaction(Auction auction, Transaction transaction) {
			}
			@Override
			public void auctionClosed(Auction auction) {
				updateAuctionClosed((HotelAuction) auction);
			}
		};
		subscribeAll(watcher);
	}

	private void subscribeAll(Watcher watcher) {
		for (int day = 1; day < 5; day++) {
			agent.getHotelAuction(day, true).addWatcher(watcher);
			agent.getHotelAuction(day, false).addWatcher(watcher);
		}
	}
	
	private void updateAuctionClosed(HotelAuction auction) {
		int day = auction.getDay();
		boolean tt = ((HotelAuction)auction).isTT();
		int i = hashForIndex(day, tt);
		auctionsClosed[i] = true;
		int won = agent.getTACAgent().getOwn(getAuctionID(tt, day));
		held[i] = won;
		for (i = 1; i <= won; i++) {
			stock.add(new HotelBooking(day, tt));
		}
	}
	
	public void clearIntentions() {
		intentions = new int[8];
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
		int[] allocated = new int[8];
		mostRecentPackages = packages;
		clearIntentions();
		boolean tt, err;
		int prefArrive, prefDepart, hotelPremium, errCount, day, i;
		int[] tempIntentions, tempAllocations;
		Client c;
		for (Package p : packages) {
			c = p.getClient();
			prefArrive = c.getPreferredArrivalDay();
			prefDepart = c.getPreferredDepartureDay();
			hotelPremium = c.getHotelPremium();
			tt = hotelPremium / (prefDepart - prefArrive) > 35; // identify if tt is worth aiming for
			errCount = 0;
			do {
				err = false;
				tempIntentions = new int[8];
				tempAllocations = new int[8];
				for (day = prefArrive; day <= prefDepart; day++) {
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
				// push changes to main intention and allocation arrays
				for (i = 1; i <= 8; i++) {
					allocated[i] = allocated[i] + tempAllocations[i];
					intentions[i] = intentions[i] + tempIntentions[i];
				}
			}
		}
		updateBids();
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
			if (auc.isClosed())
				throw new AuctionClosedException();
			int hqw = auc.getHQW();
			int dayHash = hashForIndex(day, tt);
			auc.wipeBid();
			if (intentions[dayHash] < hqw) { // on target to win surplus to requirement
				auc.modifyBidPoint(intentions[dayHash] - hqw, auc.getAskPrice() + 1);
			}
			auc.modifyBidPoint(intentions[dayHash], auc.getAskPrice() + 50);
			auc.submitBid();
		} catch (AuctionClosedException e) {
			Agent.log.fine("Attempted to update " + agent.getHotelAuction(day, tt).toString() + 
					" after it had CLOSED");
		} catch (BidInUseException e) {
			Agent.log.fine("Attempted to update " + agent.getHotelAuction(day, tt).toString() + 
					" while it was BUSY");
		}	
	}
	
	private static int hashForIndex(int day, boolean tt) {
		return day + (tt ? 4 : 0) - 1;
	}

}

class AuctionClosedException extends Exception {}