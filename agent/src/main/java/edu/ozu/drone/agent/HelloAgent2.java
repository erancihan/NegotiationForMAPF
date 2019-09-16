package edu.ozu.drone.agent;

import edu.ozu.drone.client.AgentClient;
import edu.ozu.drone.client.DroneAgent;
import edu.ozu.drone.utils.Point;

@DroneAgent
public class HelloAgent2 extends AgentClient {

    @Override
    public void init() {
        AGENT_NAME = "Hello Agent2";
        AGENT_ID   = "S006487";

        START = new Point(2, 2);
        DEST = new Point(4, 4);
    }
}
