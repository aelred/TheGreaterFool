package agent;
import java.util.*;
import java.util.logging.Logger;

import se.sics.tac.aw.*;
import se.sics.tac.util.ArgEnumerator;


public class Agent extends AgentImpl {
	
	public static final Logger log =
            Logger.getLogger(Agent.class.getName());
	
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
    
    // Auctions //
    private Map<Pair<Boolean>, FlightAuction> flightAuctions;
    private Map<Pair<Boolean>, HotelAuction> hotelAuctions;
    private Map<Pair<EntertainmentType>, EntertainmentAuction> entertainmentAuctions;

    protected void init(ArgEnumerator args) {
        log.info("Initializing");
    }

    protected String getUsage() {
        return null;
    }

    public void gameStarted() {
        log.info("Game started");

        // Create auctions
        createAuctions();

        log.info("Creating packages");
        // Create clients and packages
        packages = new ArrayList<Package>();
        
        clients = new Client[NUM_CLIENTS];
        for (int i = 0; i < clients.length; i++) {
            clients[i] = new ClientFromTAC(agent, i);
            packages.add(new Package(clients[i], 
                clients[i].getPreferredArrivalDay(), 
                clients[i].getPreferredDepartureDay()));
        }

        log.info("Creating subagents");
        // Create agents
        flightAgent = new FlightAgent(this, flightTickets);
        hotelAgent = new HotelAgent(this, hotelBookings);
        entertainmentAgent = new EntertainmentAgent(this, entertainmentTickets);

        log.info("Start subagents");
        flightAgent.fulfillPackages(packages);
        hotelAgent.fulfillPackages(packages);
        entertainmentAgent.fulfillPackages(packages);
    }

    public void gameStopped() {
        log.info("Game stopped");
    }
    
    public TACAgent getTACAgent() {
    	return agent;
    }

    private void createAuctions() {
        log.info("Creating auctions");
        flightAuctions = new HashMap<Pair<Boolean>, FlightAuction>();
        hotelAuctions = new HashMap<Pair<Boolean>, HotelAuction>();
        entertainmentAuctions = 
            new HashMap<Pair<EntertainmentType>, EntertainmentAuction>();

        for (int day = 1; day <= NUM_DAYS; day++) {
            
            if (day > 1) {
                flightAuctions.put(new Pair<Boolean>(day,false), new FlightAuction(agent, day, false));
            }

            if (day < NUM_DAYS) {
                flightAuctions.put(new Pair<Boolean>(day,true), new FlightAuction(agent, day, true));
                hotelAuctions.put(new Pair<Boolean>(day,false), new HotelAuction(agent, day, false));
                hotelAuctions.put(new Pair<Boolean>(day,true), new HotelAuction(agent, day, true));

                for (EntertainmentType type : EntertainmentType.values()) {
                    entertainmentAuctions.put(new Pair<EntertainmentType>(day,type), 
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
        return flightAuctions.get(new Pair<Boolean>(day,arrival));
    }

    public HotelAuction getHotelAuction(int day, boolean tt) {
        return hotelAuctions.get(new Pair<Boolean>(day,tt));
    }

    public EntertainmentAuction getEntertainmentAuction(int day, EntertainmentType type) {
        return entertainmentAuctions.get(new Pair<EntertainmentType>(day,type));
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
	
	@Override
	public int hashCode() {
		return (t.hashCode() * Agent.NUM_DAYS + i);
	}

}
