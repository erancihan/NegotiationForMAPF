package edu.ozu.drone.agent.sample;

import edu.ozu.drone.client.AgentClient;
import edu.ozu.drone.agent.Agent;
import edu.ozu.drone.utils.Action;
import edu.ozu.drone.utils.ActionType;
import edu.ozu.drone.utils.Point;

public class HelloAgent extends Agent
{
    @Override
    public void init()
    {
        logger.info("loading hello agent");

        AGENT_NAME = "Hello Agent";
        AGENT_ID   = "S006486";

        START = new Point(0, 1);
        DEST = new Point(0, 10);
    }

    @Override
    public Action onMakeAction()
    {
        return new Action(ActionType.OFFER, "");
    }

    @Override
    public void onReceiveState(edu.ozu.drone.utils.State state)
    {
        // receive state of negotiation sequence
    }

    public static void main(String[] args)
    {
        new AgentClient(args, new HelloAgent());
    }
}
