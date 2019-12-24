package edu.ozu.drone.agent.sample;

import edu.ozu.drone.client.AgentClient;
import edu.ozu.drone.agent.Agent;
import edu.ozu.drone.utils.Action;
import edu.ozu.drone.utils.Point;

public class HelloAgent extends Agent
{
    @Override
    public void init()
    {
        AGENT_NAME = "Hello Agent";
        AGENT_ID   = "S006486";

        START = new Point(0, 1);
        DEST = new Point(3, 1);
    }

    @Override
    public Action onMakeAction()
    {
        return new Action();
    }

    @Override
    public void onReceiveAction()
    {
        // receive action
    }

    public static void main(String[] args)
    {
        new AgentClient(args, new HelloAgent());
    }
}
