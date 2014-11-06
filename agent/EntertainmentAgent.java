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

    public void allocateTickets() {
        // TODO
        System.out.println("Clients:");
        System.out.println("AW\tAP\tMU");
        for (int i = 0; i < clients.size(); i++) {
            Client client = clients.get(i);
            System.out.printf("%3d\t%3d\t%3d\n",
                    client.getEntertainmentPremium(EntertainmentType.ALLIGATOR_WRESTLING),
                    client.getEntertainmentPremium(EntertainmentType.AMUSEMENT),
                    client.getEntertainmentPremium(EntertainmentType.MUSEUM)
            );
        }

        System.out.println("Tickets:");
        for (int i = 0; i < tickets.size(); i++) {
            System.out.println(tickets.get(i));
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
