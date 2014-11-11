package agent;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Manages allocation of {@link agent.EntertainmentTicket}s to {@link agent.Client}s.
 */
public class EntertainmentAgent {
    private List<Client> clients;
    private List<EntertainmentTicket> tickets;

    public EntertainmentAgent(List<Client> clients, List<EntertainmentTicket> tickets) {
        this.clients = clients;
        this.tickets = tickets;
    }

    protected class Allocation implements Comparable<Allocation> {
        public final Client client;
        public final EntertainmentTicket ticket;

        public Allocation(Client client, EntertainmentTicket ticket) {
            this.client = client;
            this.ticket = ticket;
        }

        public int getValue() {
            return client.getEntertainmentPremium(ticket.getType());
        }

        public int compareTo(Allocation o) {
            return Integer.compare(this.getValue(), o.getValue());
        }
    }

    private void displayProblem() {
        System.out.println("Clients:");
        System.out.println("Day\tAW\tAP\tMU");
        for (int i = 0; i < clients.size(); i++) {
            Client client = clients.get(i);
            System.out.printf("%d-%d\t%3d\t%3d\t%3d\n",
                    client.getPreferredArrivalDay(), client.getPreferredDepartureDay(),
                    client.getEntertainmentPremium(EntertainmentType.ALLIGATOR_WRESTLING),
                    client.getEntertainmentPremium(EntertainmentType.AMUSEMENT),
                    client.getEntertainmentPremium(EntertainmentType.MUSEUM)
            );
        }

        System.out.println("Tickets:");
        for (int i = 0; i < tickets.size(); i++) { System.out.println("\t" + tickets.get(i)); }
    }

    private List<Allocation> removeMatchingAllocations(List<Allocation> allocations, Allocation addedAllocation) {
        List<Allocation> newAllocations = new ArrayList<Allocation>();

        // TODO: also remove allocations to clients who already have that entertainment
        for (Allocation allocation : allocations) {
            if (!((allocation.client == addedAllocation.client
                        && allocation.ticket.getDay() == addedAllocation.ticket.getDay())
                    || (allocation.ticket == addedAllocation.ticket))) {
                newAllocations.add(allocation);
            }
        }

        return newAllocations;
    }

    public void allocateTickets() {
        displayProblem();

        // Build list of possible Allocations
        List<Allocation> allocations = new ArrayList<Allocation>();
        for (Client client : clients) {
            for (EntertainmentTicket ticket : tickets) {
                if (client.getPreferredArrivalDay() <= ticket.getDay()
                        && ticket.getDay() < client.getPreferredDepartureDay()) {
                    // TODO: use the days we intend to give the client, not their preferences
                    allocations.add(new Allocation(client, ticket));
                }
            }
        }

        // Choose the best Allocations
        java.util.Collections.sort(allocations);
        java.util.Collections.reverse(allocations);

        System.out.println("Possible allocations:");
        for (Allocation allocation : allocations) {
            System.out.printf("\t%s for $%d\n", allocation.ticket, allocation.getValue());
        }

        List<Allocation> finalAllocations = new ArrayList<Allocation>();
        while (allocations.size() > 0) {
            Allocation addedAllocation = allocations.remove(0);
            finalAllocations.add(addedAllocation);
            allocations = removeMatchingAllocations(allocations, addedAllocation);
        }

        System.out.println("Final allocations:");
        for (Allocation allocation : finalAllocations) {
            System.out.printf("\t%s for $%d\n", allocation.ticket, allocation.getValue());
        }
    }

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
        List<Client> clients = new ArrayList<Client>(8);
        for (int i = 0; i < 8; i++) {
            clients.add(new RandomClient());
        }

        // Generate tickets
        Random rnd = new Random();
        List<EntertainmentTicket> tickets = new ArrayList<EntertainmentTicket>(12);
        addRandomTickets(rnd, tickets, 1, 4);
        addRandomTickets(rnd, tickets, 2, 3);

        EntertainmentAgent entertainmentAgent = new EntertainmentAgent(clients, tickets);
        entertainmentAgent.allocateTickets();
    }
}
