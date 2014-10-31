package agent;
import se.sics.tac.aw.*;
import se.sics.tac.util.ArgEnumerator;


public class Agent extends AgentImpl {

    public static final int NUM_DAYS = 5;

    // The plane agent monitors and buys plane tickets
    private FlightAgent flightAgent = new FlightAgent();

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

    public void bidUpdated(Bid bid) {
    }

    public void bidRejected(Bid bid) {
    }

    public void bidError(Bid bid, int error) {
    }

    public void gameStarted() {
    }

    public void gameStopped() {
    }

    public void auctionClosed(int auction) {
    }

    public void transaction(Transaction transaction) {
    }
}
