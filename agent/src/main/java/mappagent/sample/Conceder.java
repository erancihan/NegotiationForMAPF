package mappagent.sample;

import edu.ozu.mapp.agent.Agent;
import edu.ozu.mapp.agent.MAPPAgent;
import edu.ozu.mapp.agent.client.AgentClient;
import edu.ozu.mapp.agent.client.helpers.WorldHandler;
import edu.ozu.mapp.utils.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SuppressWarnings("Duplicates")
@MAPPAgent
public class Conceder extends Agent {
    public Conceder()
    {
        this("Conceder 1", "CONCEDER1", new Point(2, 0), new Point(2, 10));
        isHeadless = false;
    }

    public Conceder(String agentName, String agentID, Point start, Point dest)
    {
        super(agentName, agentID, start, dest);
    }

    private List<Bid>bid_space = new ArrayList<>();

    @Override
    public void PreNegotiation()
    {
        // add current broadcast as it is the BEST outcome
//        bids.add(getBroadcastArray());

        String[][] fov = WorldHandler.getFieldOfView(this.WORLD_ID, this.AGENT_ID);
        // calculate next best possible given everything is a constraint
        ArrayList<String[]> constraints = new ArrayList<>();
        for (String[] broadcast_data : fov)
        {   // [AgentID, AgentPOS, AgentBroadcast]
            // AgentID is in the format "agent:ID"
            // do not add self as constraint
            if (broadcast_data[0].equals("agent:"+AGENT_ID))
                continue;

            String[] broadcast = broadcast_data[2].replaceAll("([\\[\\]]*)", "").split(",");
            for (int i = 0; i < broadcast.length; i++)
            {
                constraints.add(new String[]{broadcast[i], String.valueOf(i)});
            }
        }

        // Get Current Bid Space
        bid_space = GetCurrentBidSpace();
    }

    @Override
    public Action onMakeAction()
    {
        int current_tokens = WorldHandler.getTokenBalance(WORLD_ID, AGENT_ID);

        // [getToken == 0 || conflicted path and first bid is same]
        // -> if opponent insists to stay on their path

        // get opponent's bid
        Bid lastBid = null;
        for (String agentID : history.keySet()) { // get last bid of opponent
            // AgentID is in format "agent:ID"
            if (agentID.equals("agent:"+AGENT_ID))
                continue;
            // get the last bid
            String[] opponentBids = history.get("agent:" + agentID).toArray(new String[0]);
//            lastBid = new BidStruct()[opponentBids.length - 1];
        }

        if (current_tokens == 0)
        {   // if token count is > 0, it is a repeating offer
            // give second best bid
            // TODO ACCEPT CONDITION
            return new Action(this, ActionType.OFFER, bid_space.get(0).path.toStringArray());
        } else {
            // give best bid
            // -> current path
            return new Action(this, ActionType.OFFER, GetOwnBroadcastPath());
        }
    }

    public static void main(String[] args)
    {
        String AgentName = "Conceder 1";
        String AgentID   = "CONCEDER1";
        Point Start = new Point(0, 2);
        Point Dest  = new Point(10, 2);
        boolean IsHeadless = false;

        for (String arg : args) {
            if (arg.startsWith("NAME=")) AgentName = arg.replace("NAME=", "");
            if (arg.startsWith("ID=")) AgentID = arg.replace("ID=", "");
            if (arg.startsWith("START=")) Start = new Point(arg.replace("START=", ""), "-");
            if (arg.startsWith("DEST=")) Dest = new Point(arg.replace("DEST=", ""), "-");
            if (arg.equals("HEADLESS")) IsHeadless = true;
        }

        Conceder agent = new Conceder(AgentName, AgentID, Start, Dest);
        agent.isHeadless = IsHeadless;

        new AgentClient(args, agent);
    }
}
