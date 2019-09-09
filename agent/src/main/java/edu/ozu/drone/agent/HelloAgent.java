package edu.ozu.drone.agent;

import edu.ozu.drone.client.AgentClient;
import edu.ozu.drone.client.DroneAgent;
import edu.ozu.drone.utils.Point;

@DroneAgent
public class HelloAgent extends AgentClient {

    @Override
    public void init() {
        AGENT_NAME = "Hello Agent";
        AGENT_ID   = "S006486";

        START = new Point(0, 0);
        DEST = new Point(4, 4);
    }
}
