package edu.ozu.drone.agent;

import edu.ozu.drone.AgentClient;
import edu.ozu.drone.DroneAgent;

@DroneAgent
public class HelloAgent extends AgentClient {

    @Override
    public String agentID() {
        return "S006486";
    }

    @Override
    public void name() {
        System.out.println("Hello Agent");
    }
}
