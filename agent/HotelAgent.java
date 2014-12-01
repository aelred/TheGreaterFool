package agent;

import java.util.List;
import java.util.Map;

public class HotelAgent extends SubAgent<HotelBooking> {

	private boolean[] auctionsClosed = new boolean[8];
	private int[] held = new int[8];
	private int[] allocated = new int[8];
	private int[] intentions = new int[8];
	private List<Package> mostRecentPackages;

	public HotelAgent(Agent agent, List<HotelBooking> hotelStock) {
		super(agent, hotelStock);
	}

	public void clearIntentions() {
		intentions = new int[8];
		allocated = new int[8];
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
			try {
				updateBid(day, tt);
			} catch (AuctionClosedException e) {
				Agent.log.fine("Attempted to update the bid for the CLOSED hotel auction with day="
						+ Integer.toString(day) + ", towers=" + (tt ? "tt" : "ss"));
			} catch (BidInUseException e) {
				Agent.log.fine("Attempted to update the bid for the BUSY hotel auction with day="
						+ Integer.toString(day) + ", towers=" + (tt ? "tt" : "ss"));
			}			
		}
	}
	
	private void updateBid(int day, boolean tt) throws AuctionClosedException, BidInUseException {
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
	}
	
	private static int hashForIndex(int day, boolean tt) {
		return day + (tt ? 4 : 0) - 1;
	}

}

class AuctionClosedException extends Exception {}