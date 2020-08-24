package mappagent.sample;

import edu.ozu.mapp.agent.Agent;
import edu.ozu.mapp.agent.client.AgentClient;
import edu.ozu.mapp.agent.client.handlers.World;
import edu.ozu.mapp.utils.AStar;
import edu.ozu.mapp.utils.Action;
import edu.ozu.mapp.utils.ActionType;
import edu.ozu.mapp.utils.Point;

import java.util.ArrayList;
import java.util.List;

public class HelloAgent2 extends Agent
{
    public HelloAgent2()
    {
        this("Hello Agent2", "S006487", new Point(0, 2), new Point(10, 2));
        isHeadless = false;
    }

    public HelloAgent2(String agentName, String agentID, Point start, Point dest)
    {
        super(agentName, agentID, start, dest);
    }

    @Override
    public void preNegotiation() {

    }

    private int count = 0;

    @Override
    public Action onMakeAction()
    {
        if (count++ >= 2)
            return new Action(this, ActionType.ACCEPT);

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

        return new Action(this, ActionType.OFFER, bid);
    }

    public static void main(String[] args)
    {
        new AgentClient(args, new HelloAgent2());
    }
}
