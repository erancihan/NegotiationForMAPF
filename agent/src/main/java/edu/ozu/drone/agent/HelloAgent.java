package edu.ozu.drone.agent;

import edu.ozu.drone.AgentClient;
import edu.ozu.drone.DroneAgent;

@DroneAgent
public class HelloAgent extends AgentClient {

    @Override
    public void init() {
        AGENT_NAME = "Hello Agent";
        AGENT_ID   = "S006486";

        START_X = "0";
        START_Y = "0";
    }
}
