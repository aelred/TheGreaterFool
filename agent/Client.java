package agent;
import se.sics.tac.aw.*;

public class Client {

    private TACAgent agent;
    private int id;

    public Client(TACAgent agent, int id) {
        this.agent = agent;
        this.id = id;
    }
    
    public int getID() { return id; }
    
    public int getPreferredArrivalDay()   {
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
