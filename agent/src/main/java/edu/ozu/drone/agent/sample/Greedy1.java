package edu.ozu.drone.agent.sample;

import edu.ozu.drone.agent.Agent;
import edu.ozu.drone.client.AgentClient;
import edu.ozu.drone.client.handlers.World;
import edu.ozu.drone.utils.Action;
import edu.ozu.drone.utils.State;

public class Greedy1 extends Agent {
    @Override
    public void init()
    {
        AGENT_NAME = "Greedy Agent 1";
        AGENT_ID   = "GREEDY1";
    }

    @Override
    public void preNegotiation()
    {
        String[][] fov = World.getFieldOfView(this.WORLD_ID, this.AGENT_ID);
        // calculate next best possible given everything is a constraint

        // add current broadcast as it is the BEST outcome
        // add second best, which is the one with constraints

        // add them to bids list
    }

    @Override
    public Action onMakeAction()
    {
        return null;
    }

    public static void main(String[] args)
    {
        new AgentClient(args, new HelloAgent());
    }
}
