package agent;
import java.util.*;
import se.sics.tac.aw.*;
import se.sics.tac.util.ArgEnumerator;


public class Agent extends AgentImpl {

    public static final int NUM_DAYS = 5;
    public static final int NUM_CLIENTS = 8;

    private Client[] clients;
    private List<Set<Auction>> flightAuctions;
    private List<Set<Auction>> hotelAuctions;
    private List<Set<Auction>> entertainmentAuctions;

    // The plane agent monitors and buys plane tickets
    private FlightAgent flightAgent;

    protected void init(ArgEnumerator args) {
    }

    protected String getUsage() {
        return null;
    }

    public void quoteUpdated(Quote quote) {
        int auction = quote.getAuction();
        int auctionCategory = agent.getAuctionCategory(auction);

        // Give info to respective sub-agent
        switch (auctionCategory) {
            case TACAgent.CAT_FLIGHT:
                break;
            case TACAgent.CAT_HOTEL:
                break;
            case TACAgent.CAT_ENTERTAINMENT:
                break;
        }
    }

    public void bidUpdated(BidString bid) {
    }

    public void bidRejected(BidString bid) {
    }

    public void bidError(BidString bid, int error) {
    }

    public void gameStarted() {
        flightAgent = new FlightAgent();
        createAuctions();

        clients = new Client[NUM_CLIENTS];
        for (int i = 0; i < clients.length; i++) {
            clients[i] = new ClientFromTAC(agent, i);
        }
    }

    public void gameStopped() {
    }

    public void auctionClosed(int auction) {
    }

    public void transaction(Transaction transaction) {
    }

    private void createAuctions() {
        flightAuctions = new ArrayList<Set<Auction>>(NUM_DAYS);
        hotelAuctions = new ArrayList<Set<Auction>>(NUM_DAYS);
        entertainmentAuctions = new ArrayList<Set<Auction>>(NUM_DAYS);

        for (int day = 0; day < NUM_DAYS; day ++) {
            flightAuctions.add(new HashSet<Auction>());
            hotelAuctions.add(new HashSet<Auction>());
            entertainmentAuctions.add(new HashSet<Auction>());

            if (day > 0) {
                // No in-flights on first day
                flightAuctions.get(day).add(new Auction(new PlaneTicket(day, false)));
            }
            if (day < NUM_DAYS-1) {
                // No out-flights, hotels or entertainment on last day
                flightAuctions.get(day).add(new Auction(new PlaneTicket(day, true)));

                hotelAuctions.get(day).add(new Auction(new HotelBooking(day, false)));
                hotelAuctions.get(day).add(new Auction(new HotelBooking(day, true)));

                for (EntertainmentType type : EntertainmentType.values()) {
                    entertainmentAuctions.get(day).add(new Auction(
                        new EntertainmentTicket(day, type)));
                }
            }
        }
    }
}
