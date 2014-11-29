package agent;
import java.util.*;
import se.sics.tac.aw.*;
import se.sics.tac.util.ArgEnumerator;


public class Agent extends AgentImpl {

    public static final int NUM_DAYS = 5;
    public static final int NUM_CLIENTS = 8;
    
    private HotelAgent hotelAgent;

    private Client[] clients;
    private List<Package> packages;

    // Current stock
    private List<PlaneTicket> planeTickets = new ArrayList<PlaneTicket>();
    private List<HotelBooking> hotelBookings = new ArrayList<HotelBooking>();
    private List<EntertainmentTicket> entertainmentTickets = new ArrayList<EntertainmentTicket>();

    // The plane agent monitors and buys plane tickets
    private FlightAgent flightAgent;

    protected void init(ArgEnumerator args) {
    }

    protected String getUsage() {
        return null;
    }

    public void gameStarted() {
        flightAgent = new FlightAgent();
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

    private Map<Integer, Auction> auctions;

    private void addAuction(int category, int type, int day) {
        int auctionID = agent.getAuctionFor(category, type, day);
        auctions.put(auctionID, new Auction(agent, auctionID));
    }

    private void createAuctions() {
        auctions = new HashMap<Integer, Auction>();

        for (int day = 1; day <= NUM_DAYS; day++) {
            if (day > 0) addAuction(TACAgent.CAT_FLIGHT, TACAgent.TYPE_INFLIGHT, day);

            if (day < NUM_DAYS) {
                addAuction(TACAgent.CAT_FLIGHT, TACAgent.TYPE_OUTFLIGHT,   day);
                addAuction(TACAgent.CAT_HOTEL,  TACAgent.TYPE_CHEAP_HOTEL, day);
                addAuction(TACAgent.CAT_HOTEL,  TACAgent.TYPE_GOOD_HOTEL,  day);

                for (EntertainmentType type : EntertainmentType.values()) {
                    addAuction(TACAgent.CAT_ENTERTAINMENT, type.getValue(), day);
                }
            }
        }
    }

    public Auction getAuctionByID(int auctionID) {
        return auctions.get(auctionID);
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
