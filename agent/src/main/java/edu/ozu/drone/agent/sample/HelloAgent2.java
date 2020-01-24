package edu.ozu.drone.agent.sample;

import edu.ozu.drone.agent.Agent;
import edu.ozu.drone.client.AgentClient;
import edu.ozu.drone.utils.Action;
import edu.ozu.drone.utils.ActionType;
import edu.ozu.drone.utils.Point;

public class HelloAgent2 extends Agent {

    @Override
    public void init() {
        AGENT_NAME = "Hello Agent2";
        AGENT_ID   = "S006487";

        START = new Point(1, 0);
        DEST = new Point(10, 0);
    }

    @Override
    public Action onMakeAction()
    {
        return new Action(ActionType.ACCEPT);
    }

    @Override
    public void onReceiveState(edu.ozu.drone.utils.State state)
    {

    }

    public static void main(String[] args)
    {
        new AgentClient(args, new HelloAgent2());
    }
}
