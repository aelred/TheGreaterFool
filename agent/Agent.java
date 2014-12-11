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
        hotelAgent = new HotelAgent(this, hotelBookings, mainLogger.getSublogger("hotel"));
        entertainmentAgent = new EntertainmentAgent(this, entertainmentTickets, mainLogger.getSublogger("entertainment"));

        mainLogger.log("Creating packages");
        createPackages();

        mainLogger.log("Starting subagents");
        fulfillPackages();
    }

    public void gameStopped() {
        mainLogger.log("Game stopped", AgentLogger.INFO);
        
        // Tell subagents to stop
        flightAgent.gameStopped();
        hotelAgent.gameStopped();
        entertainmentAgent.gameStopped();
        
        mainLogger.save();
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

    // Return list of possible entertainment ticket allocations
    private List<EntertainmentType[]> getTicketAllocations(
        Set<Integer> days, List<EntertainmentType> types) {
        List<EntertainmentType[]> allocs = new ArrayList<EntertainmentType[]>();

        if (types.size() > 0) {
            // For a ticket type, allocate it each possible day
            List<EntertainmentType> lessTypes = 
                new ArrayList<EntertainmentType>(types);
            EntertainmentType type = lessTypes.remove(0);
            for (int day : days) {
                Set<Integer> lessDays = new HashSet<Integer>(days);
                lessDays.remove(day);
                List<EntertainmentType[]> newAllocs = 
                    getTicketAllocations(lessDays, lessTypes);
                
                for (EntertainmentType[] alloc : newAllocs) {
                    alloc[day] = type;
                }
            }
        } else {
            allocs.add(new EntertainmentType[NUM_DAYS + 1]);
        }

        return allocs;
    }

    // Return expected profit on this package
    private float getPackageOutcome(Package pack, 
            int[] arriveStock,
            int[] departStock,
            boolean towers,
            int[] hotelStock,
            Map<EntertainmentType, int[]> entStock) {

        float probFlight = 1f;
        float costFlight = 0f;

        // If a ticket or booking is already stocked, prob=1 and cost=0 so
        // we can skip it.

        // Get flight probabilities
        int arrive = pack.getArrivalDay();
        if (arriveStock[arrive] <= 0) {
            probFlight *= flightAgent.purchaseProbability(getFlightAuction(arrive, true));
            costFlight += flightAgent.estimatedPrice(getFlightAuction(arrive, true));
        }

        int depart = pack.getDepartureDay();
        if (departStock[depart] <= 0) {
            probFlight *= flightAgent.purchaseProbability(getFlightAuction(depart, false));
            costFlight += flightAgent.estimatedPrice(getFlightAuction(depart, false));
        }

        // Get hotel probabilities
        float probHotel = 1f;
        float costHotel = 0f;
        for (int day = arrive; day < depart; day++) {
            if (hotelStock[day] <= 0) {
                probHotel *= hotelAgent.purchaseProbability(getHotelAuction(day, towers));
                costHotel += hotelAgent.estimatedPrice(getHotelAuction(day, towers));
            }
        }

        // Get entertainment probabilities
        float costEnt = 0f;

        // Get all possible ticket allocations
        Set<Integer> days = new HashSet<Integer>();
        for (int day = 1; day <= NUM_DAYS; day ++) {
            days.add(day);
        }
        List<EntertainmentType> types = new ArrayList<EntertainmentType>();
        for (EntertainmentType type : EntertainmentType.values()) {
            types.add(type);
        }
        List<EntertainmentType[]> allocs = getTicketAllocations(days, types);

        for (EntertainmentType[] alloc : allocs) {
            float probThis = 1f;
            float costThis = 0f;
            for (int day = 1; day <= NUM_DAYS; day++) {
                if (alloc[day] != null) {
                    if (entStock.get(alloc[day])[day] <= 0) {
                        costThis += entertainmentAgent.estimatedPrice(
                            getEntertainmentAuction(day, alloc[day]));
                        probThis *= entertainmentAgent.purchaseProbability(
                            getEntertainmentAuction(day, alloc[day]));
                    }
                }
            }
            
            float outcome = probThis * costThis;
            if (outcome > costEnt) {
                costEnt = outcome;
            }
        }

        // Three outcomes: we buy the package at the estimated price with TT or
        // SS, OR we don't, but we still pay some cost for buying some things
        // (assume half cost of package).
        float profit = pack.potentialUtility(towers);
        float cost = costHotel + costFlight + costEnt;

        float outcome = 
            probFlight * probHotel * (profit - cost) + 
            (1f - probFlight) * (1f - probHotel) * (-cost) / 2f;
        return outcome;
    }

    private void createPackages() {
        // Create packages
        packages = new ArrayList<Package>();

        // Create a stock of owned tickets and bookings that could be allocated
        int[] arriveStock = new int[NUM_DAYS + 1];
        int[] departStock = new int[NUM_DAYS + 1];
        int[] ttStock = new int[NUM_DAYS + 1];
        int[] ssStock = new int[NUM_DAYS + 1];
        Map<EntertainmentType, int[]> entStock = 
            new HashMap<EntertainmentType, int[]>();

        for (EntertainmentType type : EntertainmentType.values()) {
            entStock.put(type, new int[NUM_DAYS + 1]);
        }

        for (int day = 0; day <= NUM_DAYS; day ++) {
            arriveStock[day] = 0;
            departStock[day] = 0;
            ttStock[day] = 0;
            ssStock[day] = 0;
            for (EntertainmentType type : EntertainmentType.values()) {
                entStock.get(type)[day] = 0;
            }
        }

        for (FlightTicket ticket : flightTickets) {
            int[] stock = ticket.isArrival() ? arriveStock : departStock;
            stock[ticket.getDay()] += 1;
        }

        for (HotelBooking booking : hotelBookings) {
            int[] stock = booking.towers ? ttStock : ssStock;
            stock[booking.getDay()] += 1;
        }

        for (EntertainmentTicket ticket : entertainmentTickets) {
            entStock.get(ticket.getType())[ticket.getDay()] += 1;
        }

        mainLogger.log("Creating packages:");

        mainLogger.log("Stock:");
        mainLogger.log("Type\tIn\tout\tTT\t\tSS\t\tE1\tE2\tE3");
        for (int day = 1; day <= NUM_DAYS; day ++) {
            float ttOpen, ssOpen;
            if (day < 5) {
                ttOpen = hotelAgent.purchaseProbability(getHotelAuction(day, true));
                ssOpen = hotelAgent.purchaseProbability(getHotelAuction(day, false));
            } else {
                ttOpen = 0f;
                ssOpen = 0f;
            }
            mainLogger.log(""+day+"\t"+arriveStock[day]+"\t"+departStock[day]+
                "\t"+ttStock[day]+"\t"+(ttOpen!=0f ? "open" : "close")+
                "\t"+ssStock[day]+"\t"+(ssOpen!=0f ? "open" : "close")+"\t"+
                entStock.get(EntertainmentType.fromValue(1))[day]+"\t"+
                entStock.get(EntertainmentType.fromValue(2))[day]+"\t"+
                entStock.get(EntertainmentType.fromValue(3))[day]);
        }

        mainLogger.log("Allocations:");
        mainLogger.log("id\tHotel\tE1\tE2\tE3\tIn\tOut\tSetIn\tSetOut\tOutcome");

        for (int i = 0; i < clients.length; i++) {
            // Select viable packages with highest expected outcome
            Package bestPackage = null;
            float bestOutcome = 0;
            boolean bestTowers = false;

            for (int arrive = 1; arrive <= NUM_DAYS-1; arrive++) {
                for (int depart = arrive + 1; depart <= NUM_DAYS; depart++) {
                    Package pack = new Package(clients[i], arrive, depart);

                    // Calculate expected outcome of package
                    // With good hotel
                    float outcomeTT = getPackageOutcome(pack, 
                        arriveStock, departStock, true, ttStock, entStock);

                    if (outcomeTT > bestOutcome) {
                        bestOutcome = outcomeTT;
                        bestPackage = pack;
                        bestTowers = true;
                    }

                    // With cheap hotel
                    float outcomeSS = getPackageOutcome(pack,
                        arriveStock, departStock, false, ssStock, entStock);

                    if (outcomeSS > bestOutcome) {
                        bestOutcome = outcomeSS;
                        bestPackage = pack;
                        bestTowers = false;
                    }
                }
            }

            // If package is null, no good package can be found,
            // so we forget this client.
            String setIn;
            String setOut;
            if (bestPackage == null) {
                setIn = "_";
                setOut = "_";
            } else {
                setIn = Integer.toString(bestPackage.getArrivalDay());
                setOut = Integer.toString(bestPackage.getDepartureDay());

                packages.add(bestPackage);

                // Remove any used stock
                int arrive = bestPackage.getArrivalDay();
                int depart = bestPackage.getDepartureDay();
                arriveStock[arrive] -= 1;
                departStock[depart] -= 1;

                int[] stock = bestTowers ? ttStock : ssStock;
                for (int day = arrive; day < depart; day++) {
                    stock[day] -= 1;
                }

                // Remove from first ticket type with stock left
                int numDays = Math.min(depart - arrive, 3);
                for (int day = 0; day < numDays; day++) {
                    for (int[] ent : entStock.values()) {
                        if (ent[arrive+day] > 0) {
                            ent[arrive+day] -= 1;
                            break;
                        }
                    }
                }
            }

            mainLogger.log(""+i+"\t"+clients[i].getHotelPremium()+"\t"+
                clients[i].getEntertainmentPremium(EntertainmentType.fromValue(1))+"\t"+
                clients[i].getEntertainmentPremium(EntertainmentType.fromValue(2))+"\t"+
                clients[i].getEntertainmentPremium(EntertainmentType.fromValue(3))+"\t"+
                clients[i].getPreferredArrivalDay()+"\t"+
                clients[i].getPreferredDepartureDay()+"\t"+
                setIn+"\t"+ setOut+"\t"+bestOutcome);
        }

    }

    private void fulfillPackages() {
        // Only tell subagents to fulfill packages if the hotel agent approves
        // and agrees the packages are feasible!
        if (hotelAgent.fulfillPackages_(packages)) {
            flightAgent.fulfillPackages(packages);
            entertainmentAgent.fulfillPackages(packages);
        } else {
            // Re-try but this is bad
            alertInfeasible();
            throw new RuntimeException("Created an invalid set of packages");
        }
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
