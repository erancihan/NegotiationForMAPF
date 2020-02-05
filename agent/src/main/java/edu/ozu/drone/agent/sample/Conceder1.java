package edu.ozu.drone.agent.sample;

import edu.ozu.drone.agent.Agent;
import edu.ozu.drone.client.AgentClient;
import edu.ozu.drone.client.handlers.World;
import edu.ozu.drone.utils.*;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("Duplicates")
public class Conceder1 extends Agent {
    public Conceder1()
    {
        this("Conceder 1", "CONCEDER1", new Point(2, 0), new Point(2, 10));
        isHeadless = false;
    }

    public Conceder1(String agentName, String agentID, Point start, Point dest)
    {
        super(agentName, agentID, start, dest);
    }

    private ArrayList<String[]> bids = new ArrayList<>();

    @Override
    public void preNegotiation()
    {
        // add current broadcast as it is the BEST outcome
//        bids.add(getBroadcastArray());

        String[][] fov = World.getFieldOfView(this.WORLD_ID, this.AGENT_ID);
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
        // calculate the alternative optimal
        List<String> path = AStar.calculateWithConstraints(POS, DEST, constraints.toArray(new String[0][3]));
        // add second best, which is the one with constraints
        String[] bid = new String[5];
        for (int i = 0; i < bid.length; i++) {
            bid[i] = path.get(i);
        }
        bids.add(bid);
        // add them to bids list
    }

    @Override
    public Action onMakeAction()
    {
        int current_tokens = World.getTokenBalance(WORLD_ID, AGENT_ID);

        // [getToken == 0 || conflicted path and first bid is same]
        // -> if opponent insists to stay on their path

        // get opponent's bid
        BidStruct lastBid = null;
        for (String agentID : history.keySet()) { // get last bid of opponent
            // AgentID is in format "agent:ID"
            if (agentID.equals("agent:"+AGENT_ID))
                continue;
            // get the last bid
            ArrayList<BidStruct> opponentBids = history.get("agent:"+agentID);
            lastBid = opponentBids.get(opponentBids.size()-1);
        }

        if (current_tokens == 0 || (lastBid != null && lastBid.token_count > 0))
        {   // if token count is > 0, it is a repeating offer
            // give second best bid
            // TODO ACCEPT CONDITION
            return new Action(this, ActionType.OFFER, bids.get(0));
        } else {
            // give best bid
            // -> current path
            return new Action(this, ActionType.OFFER, getBroadcastArray());
        }
    }

    public static void main(String[] args)
    {
        new AgentClient(args, new Conceder1());
    }
}
