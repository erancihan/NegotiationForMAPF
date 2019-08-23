package edu.ozu.drone.agent;

import edu.ozu.drone.AgentClient;
import edu.ozu.drone.DroneAgent;
import edu.ozu.drone.Point;

@DroneAgent
public class HelloAgent2 extends AgentClient {

    @Override
    public void init() {
        AGENT_NAME = "Hello Agent2";
        AGENT_ID   = "S006487";

        START = new Point(0, 0);
        DEST = new Point(4, 4);
    }
}