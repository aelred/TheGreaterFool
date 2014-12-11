package agent.entertainment;

import java.util.*;
import java.util.logging.Logger;

import agent.Agent;
import agent.Auction;
import agent.Client;
import agent.Package;
import agent.RandomClient;
import agent.SubAgent;
import agent.Package.PackageFullException;
import agent.logging.AgentLogger;

/**
 * Manages allocation of {@link agent.entertainment.EntertainmentTicket}s to {@link agent.Client}s.
 */
public class EntertainmentAgent extends SubAgent<EntertainmentTicket> {

    private List<Package> packages;
    private List<EntertainmentBuyer> buyers = new ArrayList<EntertainmentBuyer>();
    private List<EntertainmentSeller> sellers = new ArrayList<EntertainmentSeller>();

    private static final float PROFIT_FACTOR = 0.2f;
    private static final float TICKET_SELL_PRICE = 100f;

    public EntertainmentAgent(Agent agent, List<EntertainmentTicket> stock, AgentLogger logger) {
        super(agent, stock, logger);
        logger.log("EntertainmentAgent constructed.");
    }

    protected class Allocation implements Comparable<Allocation> {
        public final Package pkg;
        public final EntertainmentTicket ticket;

        public Allocation(Package pkg, EntertainmentTicket ticket) {
            this.pkg = pkg;
            this.ticket = ticket;
        }

        public int getValue() {
            return pkg.getClient().getEntertainmentPremium(ticket.getType());
        }

        public int compareTo(Allocation o) {
            return Integer.compare(this.getValue(), o.getValue());
        }

        /**
         * Checks for conflicts between the two allocations. A conflict is where it would not make sense for both
         * allocations to be made (i.e. a client would hold two tickets for the same day or entertainment).
         * @return true if the given allocation conflicts with the current allocation; else false.
         */
        public boolean conflictsWith(Allocation other) {
            return (this.pkg == other.pkg && (
                        this.ticket.getDay() == other.ticket.getDay()
                        || this.ticket.getType() == other.ticket.getType()
                    ))
                    || this.ticket == other.ticket;
        }

        public void perform() {
            pkg.setEntertainmentTicket(ticket);
            ticket.setAssociatedPackage(pkg);
        }
    }

    public void gameStopped() {
        buyers.clear();
        sellers.clear();
    }

    private void cancelBidders(List<? extends EntertainmentBidder> bidders) {
        for (EntertainmentBidder bidder : bidders) {
            bidder.cancelBid();
        }
        bidders.clear();
    }

    @Override
    public void clearPackages() {
        for (Package pkg : this.packages) {
            pkg.clearEntertainmentTickets();
        }
        for (EntertainmentTicket ticket : stock) {
            ticket.clearAssociatedPackage();
        }

        cancelBidders(buyers);
        cancelBidders(sellers);
    }

    private List<Allocation> possibleAllocations() {
        List<Allocation> allocations = new ArrayList<Allocation>();
        logger.log("We already own " + stock.size() + " tickets.");
        for (Package pkg : packages) {
            for (EntertainmentTicket ticket : stock) {
                if (pkg.getArrivalDay() <= ticket.getDay()
                        && ticket.getDay() < pkg.getDepartureDay()) {
                    allocations.add(new Allocation(pkg, ticket));
                }
            }
        }
        return allocations;
    }

    private List<Allocation> removeMatchingAllocations(List<Allocation> allocations, Allocation addedAllocation) {
        List<Allocation> newAllocations = new ArrayList<Allocation>();

        for (Allocation allocation : allocations) {
            if (!addedAllocation.conflictsWith(allocation)) {
                newAllocations.add(allocation);
            }
        }

        return newAllocations;
    }

    private List<Allocation> chooseBestAllocations(List<Allocation> allocations) {
        java.util.Collections.sort(allocations);
        java.util.Collections.reverse(allocations);

        List<Allocation> bestAllocations = new ArrayList<Allocation>();
        while (allocations.size() > 0) {
            Allocation addedAllocation = allocations.remove(0);
            bestAllocations.add(addedAllocation);
            allocations = removeMatchingAllocations(allocations, addedAllocation);
        }

        return bestAllocations;
    }

    /** Called by an {@link agent.entertainment.EntertainmentBuyer} when it obtains a ticket from an auction.
     *
     * @param bidder The {@link agent.entertainment.EntertainmentBuyer}.
     * @param ticket The {@link agent.entertainment.EntertainmentTicket} that was bought.
     */
    public void ticketWon(EntertainmentBuyer bidder, EntertainmentTicket ticket) {
        logger.log("Won ticket: " + ticket);
        this.stock.add(ticket);
    }

    /** Called by an {@link agent.entertainment.EntertainmentSeller} when it sells one or more tickets in an auction.
     *
     * @param seller The {@link agent.entertainment.EntertainmentSeller}.
     * @param tickets A {@link java.util.List} of the {@link agent.entertainment.EntertainmentTicket}s sold.
     */
    public void ticketsSold(EntertainmentSeller seller, List<EntertainmentTicket> tickets) {
        logger.log("Sold " + tickets.size() + " tickets (" + tickets.get(0) + ").");
        this.stock.removeAll(tickets);
    }

    private void bidFor(Package pkg, int day, EntertainmentType type, float bidPrice) {
        EntertainmentAuction auction = agent.getEntertainmentAuction(day, type);
        logger.log("Bidding for a ticket to " + type + " on " + day);
        EntertainmentBuyer buyer = new EntertainmentBuyer(this, auction, pkg, bidPrice, logger.getSublogger("bidder"));
        buyers.add(buyer);
    }

    private void bidForUnfilledSlots() {
        for (Package pkg : packages) {
            try {
                // TODO: fill more valuable slots first
                for (EntertainmentType type : EntertainmentType.values()) {
                    if (pkg.getEntertainmentTicket(type) == null) {
                        int day = pkg.reserveDay();
                        float bidPrice = pkg.getClient().getEntertainmentPremium(type) * (1 - PROFIT_FACTOR);
                        bidFor(pkg, day, type, bidPrice);
                    }
                }
            } catch (Package.PackageFullException ex) { }  // We can't fit anything else in the package, so move on
        }
    }

    private void sellUnusedTickets() {
        Map<EntertainmentType, List<List<EntertainmentTicket>>> unusedTickets =
                new HashMap<EntertainmentType, List<List<EntertainmentTicket>>>();
                // Stores lists of tickets to be sold by entertainment type and (zero-based) day
        for (EntertainmentType type : EntertainmentType.values()) {
            unusedTickets.put(type, new ArrayList<List<EntertainmentTicket>>(Agent.NUM_DAYS));
            for (int zbDay = 0; zbDay < Agent.NUM_DAYS; zbDay++) {
                unusedTickets.get(type).add(new ArrayList<EntertainmentTicket>(4));
            }
        }

        for (EntertainmentTicket ticket : stock) {
            if (ticket.getAssociatedPackage() == null) {
                List<EntertainmentTicket> ticketList = unusedTickets.get(ticket.getType()).get(ticket.getDay() - 1);
                ticketList.add(ticket);
            }
        }

        for (EntertainmentType type : EntertainmentType.values()) {
            for (int zbDay = 0; zbDay < Agent.NUM_DAYS; zbDay++) {
                List<EntertainmentTicket> ticketList = unusedTickets.get(type).get(zbDay);
                if (ticketList.size() == 0) continue;

                logger.log("Selling " + ticketList.size() + " x " + ticketList.get(0));
                EntertainmentSeller seller = new EntertainmentSeller(this, ticketList, TICKET_SELL_PRICE,
                        logger.getSublogger("seller"));
                sellers.add(seller);
            }
        }
    }

    public void fulfillPackages(List<Package> packages) {
        this.packages = packages;

        List<Allocation> allocations = possibleAllocations();
        List<Allocation> bestAllocations = chooseBestAllocations(allocations);

        System.out.println("Best allocations:");
        for (Allocation allocation : bestAllocations) {
            allocation.perform();
            System.out.printf("\t%s for $%d\n", allocation.ticket, allocation.getValue());
        }
        logger.log("Made " + bestAllocations.size() + " allocations out of a possible " + allocations.size() + ".");

        bidForUnfilledSlots();

        sellUnusedTickets();
    }

    @Override
    public float purchaseProbability(Auction<?> auction) {
        return 1;
    }

    @Override
    public float estimatedPrice(Auction<?> auction) {
        // Unused in top agent
        return 100;
    }

    // Static test methods //

    private static void addTickets(List<EntertainmentTicket> tickets, int count, int day, EntertainmentType type) {
        for (int i = 0; i < count; i++) {
            tickets.add(new EntertainmentTicket(day, type));
        }
    }

    private static void addRandomTickets(Random rnd, List<EntertainmentTicket> tickets, int day1, int day2) {
        EntertainmentType type1 = EntertainmentType.randomType(rnd);
        addTickets(tickets, 4, (rnd.nextInt(2) == 0) ? day1 : day2, type1);
        addTickets(tickets, 2, (rnd.nextInt(2) == 0) ? day1 : day2, EntertainmentType.randomType(rnd, type1));
    }

    /** Tests the {@link agent.entertainment.EntertainmentAgent} class with some {@link agent.RandomClient}s and
     * {@link agent.entertainment.EntertainmentTicket}s assigned according to the spec. */
    public static void main(String[] args) {
        // Create random clients
        List<Package> packages = new ArrayList<Package>(8);
        for (int i = 0; i < 8; i++) {
            packages.add(new Package(new RandomClient()));
        }

        // Generate tickets
        Random rnd = new Random();
        List<EntertainmentTicket> tickets = new ArrayList<EntertainmentTicket>(12);
        addRandomTickets(rnd, tickets, 1, 4);
        addRandomTickets(rnd, tickets, 2, 3);

        // Display the problem
        System.out.println("Clients:");
        System.out.println("Day\tAW\tAP\tMU");
        for (int i = 0; i < packages.size(); i++) {
            Client client = packages.get(i).getClient();
            System.out.printf("%d-%d\t%3d\t%3d\t%3d\n",
                    client.getPreferredArrivalDay(), client.getPreferredDepartureDay(),
                    client.getEntertainmentPremium(EntertainmentType.ALLIGATOR_WRESTLING),
                    client.getEntertainmentPremium(EntertainmentType.AMUSEMENT),
                    client.getEntertainmentPremium(EntertainmentType.MUSEUM)
            );
        }

        System.out.println("Tickets:");
        for (int i = 0; i < tickets.size(); i++) { System.out.println("\t" + tickets.get(i)); }
        EntertainmentAgent entertainmentAgent = new EntertainmentAgent(new Agent(), tickets, 
        		new AgentLogger().getSublogger("entertainment.testing"));
        entertainmentAgent.fulfillPackages(packages);
    }
}
