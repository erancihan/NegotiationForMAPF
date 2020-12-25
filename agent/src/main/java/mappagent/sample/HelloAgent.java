package mappagent.sample;

import edu.ozu.mapp.agent.Agent;
import edu.ozu.mapp.agent.MAPPAgent;
import edu.ozu.mapp.agent.client.AgentClient;
import edu.ozu.mapp.agent.client.models.Contract;
import edu.ozu.mapp.utils.*;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("Duplicates")
@MAPPAgent
public class HelloAgent extends Agent
{
    public HelloAgent()
    {
        this("Hello Agent", "S006486", new Point(2, 0), new Point(2, 10));

        logger.info("loading hello agent");
        isHeadless = false;
    }

    public HelloAgent(String agentName, String agentID, Point start, Point dest)
    {
        super(agentName, agentID, start, dest);
    }

    @Override
    public void PreNegotiation(State state) {

    }

    @Override
    public Action onMakeAction(Contract contract)
    {

        String[][] fov = GetFieldOfView();
//        System.out.println("fov ha1 > " + Arrays.deepToString(fov));

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

        List<String> path = new AStar().calculate(POS, DEST, constraints.toArray(new String[0][3]), this.dimensions, time);
        String[] bid = new String[5];
        for (int i = 0; i < bid.length; i++) {
            bid[i] = path.get(i);
        }

        return new Action(this, ActionType.OFFER, bid);
    }

    @Override
    public void PostNegotiation() {
        logger.debug("history:" + history);
    }

    public static void main(String[] args)
    {
        new AgentClient(args, new HelloAgent());
    }
}
