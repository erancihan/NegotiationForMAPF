package edu.ozu.drone.agent.sample;

import edu.ozu.drone.agent.Agent;
import edu.ozu.drone.client.AgentClient;
import edu.ozu.drone.client.handlers.World;
import edu.ozu.drone.utils.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HelloAgent2 extends Agent {

    @Override
    public void init() {
        AGENT_NAME = "Hello Agent2";
        AGENT_ID   = "S006487";

        START = new Point(0, 2);
        DEST = new Point(10, 2);
    }

    private int count = 0;

    @Override
    public Action onMakeAction()
    {
        if (count++ >= 2)
            return new Action(ActionType.ACCEPT);

        String[][] fov = World.getFieldOfView(this.WORLD_ID, this.AGENT_ID);

        ArrayList<String[]> constraints = new ArrayList<>();
        for (String[] broadcast_data : fov)
        {
            String[] broadcast = broadcast_data[2].replaceAll("([\\[\\]]*)", "").split(",");
            for (int i = 0; i < broadcast.length; i++)
            {
                // if target is not the agent in negotiation
                // opponent id can be extracted from negotiation state
                // >> skipping for now
                constraints.add(new String[]{broadcast[i], String.valueOf(i)});
            }
        }

        List<String> path = AStar.calculateWithConstraints(POS, DEST, constraints.toArray(new String[0][3]));
        String[] bid = new String[5];
        for (int i = 0; i < bid.length; i++) {
            bid[i] = path.get(i);
        }

        String _bid = Utils.toString(bid, ",");

        return new Action(ActionType.OFFER, _bid);
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
