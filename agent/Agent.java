package agent;
import java.util.*;
import se.sics.tac.aw.*;
import se.sics.tac.util.ArgEnumerator;


public class Agent extends AgentImpl {
	
    public static final int NUM_DAYS = 5;
    public static final int NUM_CLIENTS = 8;
    

    private Client[] clients;
    private List<Package> packages;

    // Current stock
    private List<FlightTicket> flightTickets = new ArrayList<FlightTicket>();
    private List<HotelBooking> hotelBookings = new ArrayList<HotelBooking>();
    private List<EntertainmentTicket> entertainmentTickets = new ArrayList<EntertainmentTicket>();

    // SubAgents
    // The plane agent monitors and buys plane tickets
    private FlightAgent flightAgent;
    private HotelAgent hotelAgent;
    private EntertainmentAgent entertainmentAgent;

    protected void init(ArgEnumerator args) {
    }

    protected String getUsage() {
        return null;
    }

    public void gameStarted() {
        flightAgent = new FlightAgent(this, flightTickets);
        createAuctions();

        packages = new ArrayList<Package>();
        
        clients = new Client[NUM_CLIENTS];
        for (int i = 0; i < clients.length; i++) {
            clients[i] = new ClientFromTAC(agent, i);
            packages.add(new Package(clients[i]));
        }
    }

    public void gameStopped() {
    }

    // Auctions //

    private Map<Integer, Map<Boolean, FlightAuction>> flightAuctions;
    private Map<Integer, Map<Boolean, HotelAuction>> hotelAuctions;
    private Map<Integer, Map<EntertainmentType, EntertainmentAuction>> 
        entertainmentAuctions;

    private void createAuctions() {
        flightAuctions = new HashMap<Integer, Map<Boolean, FlightAuction>>();
        hotelAuctions = new HashMap<Integer, Map<Boolean, HotelAuction>>();
        entertainmentAuctions = 
            new HashMap<Integer, Map<EntertainmentType, EntertainmentAuction>>();

        for (int day = 1; day <= NUM_DAYS; day++) {
            flightAuctions.put(day, new HashMap<Boolean, FlightAuction>());
            hotelAuctions.put(day, new HashMap<Boolean, HotelAuction>());
            entertainmentAuctions.put(day,
                new HashMap<EntertainmentType, EntertainmentAuction>());

            if (day > 1) {
                flightAuctions.get(day).put(false, new FlightAuction(agent, day, false));
            }

            if (day < NUM_DAYS) {
                flightAuctions.get(day).put(true, new FlightAuction(agent, day, true));
                hotelAuctions.get(day).put(false, new HotelAuction(agent, day, false));
                hotelAuctions.get(day).put(true, new HotelAuction(agent, day, true));

                for (EntertainmentType type : EntertainmentType.values()) {
                    entertainmentAuctions.get(day).put(type, 
                        new EntertainmentAuction(agent, day, type));
                }
            }
        }
    }

    private Auction getAuctionByID(int auctionID) {
        int day = TACAgent.getAuctionDay(auctionID);
        int type = TACAgent.getAuctionType(auctionID);
        switch (TACAgent.getAuctionCategory(auctionID)) {
            case TACAgent.CAT_FLIGHT:
                return getFlightAuction(day, type==TACAgent.TYPE_INFLIGHT);
            case TACAgent.CAT_HOTEL:
                return getHotelAuction(day, type==TACAgent.TYPE_GOOD_HOTEL);
            case TACAgent.CAT_ENTERTAINMENT:
                return getEntertainmentAuction(day, 
                    EntertainmentType.fromValue(type));
            default:
                throw new IllegalArgumentException("auctionID is invalid");
        }
    }

    public FlightAuction getFlightAuction(int day, boolean arrival) {
        return flightAuctions.get(day).get(arrival);
    }

    public HotelAuction getHotelAuction(int day, boolean towers) {
        return hotelAuctions.get(day).get(towers);
    }

    public EntertainmentAuction getEntertainmentAuction(int day, EntertainmentType type) {
        return entertainmentAuctions.get(day).get(type);
    }

    /** Called when new information about the quotes on the auction (quote.getAuction()) arrives. */
    public void quoteUpdated(Quote quote) {
        int auction = quote.getAuction();
        getAuctionByID(auction).fireQuoteUpdated(quote);
    }

    /** Called when new information about the quotes on all auctions for the auction
     * category has arrived (quotes for a specific type of auctions are often requested at once). */
    public void quoteUpdated(int auctionCategory) {
    }

    /** Called when the TACAgent has received an answer on a bid query/submission
     * (new information about the bid is available)
     */
    public void bidUpdated(BidString bid) {
        getAuctionByID(bid.getAuction()).fireBidUpdated(bid);
    }

    /** Called when the bid has been rejected (reason is bid.getRejectReason()) */
    public void bidRejected(BidString bid) {
        getAuctionByID(bid.getAuction()).fireBidRejected(bid);
    }

    /** Called when a submitted bid contained errors (error represent error status - commandStatus) */
    public void bidError(BidString bid, int error) {
        getAuctionByID(bid.getAuction()).fireBidError(bid, error);
    }

    /** Called when a transaction occurs (when the agent wins an auction). */
    public void transaction(Transaction transaction) {
        getAuctionByID(transaction.getAuction()).fireTransaction(transaction);
    }

    /** Called when the auction with ID "auction" closes. */
    public void auctionClosed(int auction) {
        getAuctionByID(auction).fireClosed();
    }

}

class Pair<T> {

	private int i;
	private T t;
	
	public Pair(int i, T t) {
		this.i = i;
		this.t = t;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public boolean equals(Object o) {
		if (getClass() != o.getClass())
			return false;
		return (this.i == ((Pair<T>)o).i) && (this.t.equals(((Pair<T>)o).t));
	}

}
