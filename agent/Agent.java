package agent;
import se.sics.tac.aw.*;
import se.sics.tac.util.ArgEnumerator;


public class Agent extends AgentImpl {

    public static final int NUM_DAYS = 5;
    public static final int NUM_CLIENTS = 8;

    private Client[] clients;

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
}
