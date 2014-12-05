package agent;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

/**
 * Manages allocation of {@link agent.EntertainmentTicket}s to {@link agent.Client}s.
 */
public class EntertainmentAgent extends SubAgent<EntertainmentTicket> {
    public static final Logger log = Logger.getLogger(Agent.log.getName() + ".entertainment");

    private boolean firstRun = true;
    private List<Package> packages;
    private List<EntertainmentBidder> bidders = new ArrayList<EntertainmentBidder>();

    private static final float PROFIT_FACTOR = 0.2f;

    public EntertainmentAgent(Agent agent, List<EntertainmentTicket> stock) {
        super(agent, stock);
        log.info("EntertainmentAgent constructed.");
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
        bidders.clear();
    }

    private List<Allocation> possibleAllocations() {
        List<Allocation> allocations = new ArrayList<Allocation>();
        log.info("We already own " + stock.size() + " tickets.");
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

    /** Called by an {@link agent.EntertainmentBidder} when it obtains a ticket from an auction.
     *
     * @param bidder The {@link agent.EntertainmentBidder}.
     * @param ticket The {@link agent.EntertainmentTicket} that was bought.
     */
    public void ticketWon(EntertainmentBidder bidder, EntertainmentTicket ticket) {
        log.info("Won ticket: " + ticket);
        this.stock.add(ticket);
    }

    private void bidFor(Package pkg, int day, EntertainmentType type, float bidPrice) {
        EntertainmentAuction auction = agent.getEntertainmentAuction(day, type);
        log.info("Bidding for a ticket to " + type + " on " + day);
        EntertainmentBidder bidder = new EntertainmentBidder(this, auction, pkg, bidPrice);
        bidders.add(bidder);
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
        List<EntertainmentTicket> unusedTickets = new ArrayList<EntertainmentTicket>();
        for (EntertainmentTicket ticket : stock) {
            if (ticket.getAssociatedPackage() == null) {
                unusedTickets.add(ticket);
            }
        }

        // TODO: sell the tickets
        System.out.println("Tickets to be sold:");
        for (EntertainmentTicket ticket : unusedTickets) {
            System.out.println("\t" + ticket);
        }
    }

    public void fulfillPackages(List<Package> packages) {
        if (!firstRun) {
            for (Package pkg : this.packages) {
                pkg.clearEntertainmentTickets();
            }
            for (EntertainmentTicket ticket : stock) {
                ticket.clearAssociatedPackage();
            }

            for (EntertainmentBidder bidder : bidders) {
                bidder.cancelBid();
            }
            bidders.clear();
            // TODO: clear any untaken sell bids
        }
        firstRun = false;
        this.packages = packages;

        List<Allocation> allocations = possibleAllocations();
        List<Allocation> bestAllocations = chooseBestAllocations(allocations);

        System.out.println("Best allocations:");
        for (Allocation allocation : bestAllocations) {
            allocation.perform();
            System.out.printf("\t%s for $%d\n", allocation.ticket, allocation.getValue());
        }
        log.info("Made " + bestAllocations.size() + " allocations out of a possible " + allocations.size() + ".");

        bidForUnfilledSlots();

        sellUnusedTickets();
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

    /** Tests the {@link agent.EntertainmentAgent} class with some {@link agent.RandomClient}s and
     * {@link agent.EntertainmentTicket}s assigned according to the spec. */
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
        EntertainmentAgent entertainmentAgent = new EntertainmentAgent(new Agent(), tickets);
        entertainmentAgent.fulfillPackages(packages);
    }
}
