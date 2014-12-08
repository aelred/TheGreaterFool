package agent;
import java.util.*;
import java.util.logging.Logger;

import agent.entertainment.EntertainmentAgent;
import agent.entertainment.EntertainmentAuction;
import agent.entertainment.EntertainmentTicket;
import agent.entertainment.EntertainmentType;
import agent.flight.FlightAgent;
import agent.flight.FlightTicket;
import agent.flight.FlightAuction;
import agent.hotel.*;
import agent.logging.AgentLogger;
import se.sics.tac.aw.*;
import se.sics.tac.util.ArgEnumerator;


public class Agent extends AgentImpl {
	
	public static final AgentLogger mainLogger = new AgentLogger();
	
    public static final int NUM_DAYS = 5;
    public static final int NUM_CLIENTS = 8;
    
    private Client[] clients;
    private List<Package> packages;

    // Current stock
    private List<FlightTicket> flightTickets;
    private List<HotelBooking> hotelBookings;
    private List<EntertainmentTicket> entertainmentTickets;

    // SubAgents
    // The plane agent monitors and buys plane tickets
    private FlightAgent flightAgent;
    private HotelAgent hotelAgent;
    private EntertainmentAgent entertainmentAgent;
    
    // Auctions //
    private Map<Pair<Boolean>, FlightAuction> flightAuctions;
    private Map<Pair<Boolean>, HotelAuction> hotelAuctions;
    private Map<Pair<EntertainmentType>, EntertainmentAuction> entertainmentAuctions;
    
    private HotelHistory hotelHist = new HotelHistory();

    public static void logMessage(String identifier, String message) {
    	Logger.getLogger(identifier).info(message);
    }
    
    protected void init(ArgEnumerator args) {
        mainLogger.log("Initialising", AgentLogger.INFO);
    }

    protected String getUsage() {
        return null;
    }
    
    private void takeStock() {
        flightTickets = new ArrayList<FlightTicket>();
        hotelBookings = new ArrayList<HotelBooking>();
        entertainmentTickets = new ArrayList<EntertainmentTicket>();

        for (FlightAuction auction : flightAuctions.values()) {
            for (int i = 0; i < agent.getOwn(auction.getAuctionID()); i++) {
                flightTickets.add(auction.getBuyable());
            }
        }

        for (HotelAuction auction : hotelAuctions.values()) {
            for (int i = 0; i < agent.getOwn(auction.getAuctionID()); i++) {
                hotelBookings.add(auction.getBuyable());
            }
        }

        for (EntertainmentAuction auction : entertainmentAuctions.values()) {
            for (int i = 0; i < agent.getOwn(auction.getAuctionID()); i++) {
                entertainmentTickets.add(auction.getBuyable());
            }
        }
    }

    public void gameStarted() {
        mainLogger.log("Game started");
        
        // Create auctions
        createAuctions();

        // Create clients
        clients = new Client[NUM_CLIENTS];
        for (int i = 0; i < clients.length; i++) {
            clients[i] = new ClientFromTAC(agent, i);
        }

        takeStock();

        mainLogger.log("Creating subagents");
        // Create agents
        flightAgent = new FlightAgent(this, flightTickets, mainLogger.getSublogger("flight"));
        hotelAgent = new HotelAgent(this, hotelBookings, hotelHist, mainLogger.getSublogger("hotel"));
        entertainmentAgent = new EntertainmentAgent(this, entertainmentTickets, mainLogger.getSublogger("entertainment"));

        mainLogger.log("Creating packages");
        createPackages();

        mainLogger.log("Starting subagents");
        fulfillPackages();
    }

    public void gameStopped() {
        mainLogger.log("Game stopped", AgentLogger.INFO);
        mainLogger.save();

        // Tell subagents to stop
        flightAgent.gameStopped();
        hotelAgent.gameStopped();
        entertainmentAgent.gameStopped();
    }

    /**
     * called when the current list of packages contains an infeasible package
     */
    public void alertInfeasible() {
        mainLogger.log("Updating packages after feasibility lost");
        //mainLogger.logStack(AgentLogger.INFO);
    	
    	// Tell subagents to drop everything
        flightAgent.clearPackages();
        hotelAgent.clearPackages();
        entertainmentAgent.clearPackages();
        
        // Re-create feasible packages
        createPackages();

        // Re-assign packages to agents
        fulfillPackages();
    }
    
    public TACAgent getTACAgent() {
    	return agent;
    }

    public long getTime() {
        return agent.getGameTime();
    }

    // Return expected profit on this package
    private float getPackageOutcome(Package pack, 
            int[] arriveStock,
            int[] departStock,
            int[] hotelStock) {
        float prob = 1f;
        float cost = 0f;

        // If a ticket or booking is already stocked, prob=1 and cost=0 so
        // we can skip it.

        // Get flight probabilities
        int arrive = pack.getArrivalDay();
        if (arriveStock[arrive] <= 0) {
            prob *= flightAgent.purchaseProbability(getFlightAuction(arrive, true));
            cost += flightAgent.estimatedPrice(getFlightAuction(arrive, true));
        }

        int depart = pack.getDepartureDay();
        if (departStock[depart] <= 0) {
            prob *= flightAgent.purchaseProbability(getFlightAuction(depart, false));
            cost += flightAgent.estimatedPrice(getFlightAuction(depart, false));
        }

        // Get hotel probabilities
        for (int day = arrive; day < depart; day++) {
            if (hotelStock[day] <= 0) {
                float probTT = hotelAgent.purchaseProbability(getHotelAuction(day, true));
                float costTT = hotelAgent.estimatedPrice(getHotelAuction(day, true));
                float probSS = hotelAgent.purchaseProbability(getHotelAuction(day, false));
                float costSS = hotelAgent.estimatedPrice(getHotelAuction(day, false));
                prob *= probTT + probSS - (probTT * probSS);
                cost += costTT * probTT + costSS * probSS - 
                    ((costTT + costSS) * probTT * probSS / 2f);
            }
        }

        // Two outcomes: we buy the package at the estimated price
        // OR we don't, but we still pay some cost for buying some things
        // (assume half cost of package).
        float goodOutcome = pack.potentialUtility() - cost;
        float badOutcome = -cost / 2f;
        return prob * goodOutcome + (1f - prob) * badOutcome;
    }

    private void createPackages() {
        // Create packages
        packages = new ArrayList<Package>();

        // Create a stock of owned tickets and bookings that could be allocated
        int[] arriveStock = new int[NUM_DAYS + 1];
        int[] departStock = new int[NUM_DAYS + 1];
        int[] hotelStock = new int[NUM_DAYS + 1];
        for (int day = 0; day <= NUM_DAYS; day ++) {
            arriveStock[day] = 0;
            departStock[day] = 0;
            hotelStock[day] = 0;
        }

        for (FlightTicket ticket : flightTickets) {
            int[] stock = ticket.isArrival() ? arriveStock : departStock;
            stock[ticket.getDay()] += 1;
        }

        for (HotelBooking booking : hotelBookings) {
            hotelStock[booking.getDay()] += 1;
        }
        
        for (int i = 0; i < clients.length; i++) {
            // Select viable packages with highest expected outcome
            Package bestPackage = null;
            float bestOutcome = 0;

            for (int arrive = 1; arrive <= NUM_DAYS-1; arrive++) {
                for (int depart = arrive + 1; depart <= NUM_DAYS; depart++) {
                    Package pack = new Package(clients[i], arrive, depart);

                    // Calculate expected outcome of package
                    float outcome = getPackageOutcome(
                            pack, arriveStock, departStock, hotelStock);

                    if (outcome > bestOutcome) {
                        bestOutcome = outcome;
                        bestPackage = pack;
                    }
                }
            }

            // If package is null, no good package can be found,
            // so we forget this client.
            if (bestPackage != null) {
                packages.add(bestPackage);

                // Remove any used stock
                int arrive = bestPackage.getArrivalDay();
                int depart = bestPackage.getDepartureDay();
                arriveStock[arrive] -= 1;
                departStock[depart] -= 1;

                for (int day = arrive; day < depart; day++) {
                    hotelStock[day] -= 1;
                }
            }
        }
    }

    private void fulfillPackages() {
        flightAgent.fulfillPackages(packages);
        hotelAgent.fulfillPackages(packages);
        entertainmentAgent.fulfillPackages(packages);
    }

    private void createAuctions() {
    	mainLogger.log("Creating auctions");
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

    private Auction<?> getAuctionByID(int auctionID) {
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

    public EntertainmentAuction getEntertainmentAuction(EntertainmentTicket ticket) {
        return getEntertainmentAuction(ticket.getDay(), ticket.getType());
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
