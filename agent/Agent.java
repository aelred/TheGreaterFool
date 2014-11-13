package agent;
import java.util.*;
import se.sics.tac.aw.*;
import se.sics.tac.util.ArgEnumerator;


public class Agent extends AgentImpl {

    public static final int NUM_DAYS = 5;
    public static final int NUM_CLIENTS = 8;

    private Client[] clients;
    private Map<Buyable, Auction> auctions;

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
        // Make a list of every buyable
        List<Buyable> buyables = new ArrayList<Buyable>();

        for (int day = 0; day < NUM_DAYS; day ++) {

            if (day > 0) {
                // No in-flights on first day
                buyables.add(new PlaneTicket(day, false));
            }
            if (day < NUM_DAYS-1) {
                // No out-flights, hotels or entertainment on last day
                buyables.add(new PlaneTicket(day, true));

                buyables.add(new HotelBooking(day, false));
                buyables.add(new HotelBooking(day, true));

                for (EntertainmentType type : EntertainmentType.values()) {
                    buyables.add(new EntertainmentTicket(day, type));
                }
            }
        }

        // Make an auction for every buyable
        auctions = new HashMap<Buyable, Auction>();

        for (Buyable buyable : buyables) {
            auctions.put(buyable, new Auction(buyable));
        }
    }
}
