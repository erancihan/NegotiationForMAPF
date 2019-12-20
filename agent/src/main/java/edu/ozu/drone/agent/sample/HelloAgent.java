package edu.ozu.drone.agent.sample;

import edu.ozu.drone.client.AgentClient;
import edu.ozu.drone.client.DroneAgent;
import edu.ozu.drone.utils.Action;
import edu.ozu.drone.utils.Point;

@DroneAgent
public class HelloAgent extends AgentClient
{
    @Override
    public void init()
    {
        AGENT_NAME = "Hello Agent";
        AGENT_ID   = "S006486";

        START = new Point(0, 1);
        DEST = new Point(3, 1);
    }

    public Action onMakeAction()
    {
        return new Action();
    }

    public void onReceiveAction()
    {
        // receive action
    }
}
