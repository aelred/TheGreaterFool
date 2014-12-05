package agent;

import agent.entertainment.EntertainmentType;
import se.sics.tac.aw.TACAgent;

public class ClientFromTAC extends Client {

    private final TACAgent agent;
    private final int id;

    public ClientFromTAC(TACAgent agent, int id) {
        this.agent = agent;
        this.id = id;
    }

    public int getID() { return id; }

    public int getPreferredArrivalDay() {
        return agent.getClientPreference(id, TACAgent.ARRIVAL);
    }
    public int getPreferredDepartureDay() {
        return agent.getClientPreference(id, TACAgent.DEPARTURE);
    }
    public int getHotelPremium() { return agent.getClientPreference(id, TACAgent.HOTEL_VALUE); }

    public int getEntertainmentPremium(EntertainmentType type) {
        return agent.getClientPreference(id, type.getTACPreferenceQueryValue());
    }
}
