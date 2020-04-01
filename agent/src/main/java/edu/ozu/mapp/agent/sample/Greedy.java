package edu.ozu.mapp.agent.sample;

import edu.ozu.mapp.agent.Agent;
import edu.ozu.mapp.client.AgentClient;
import edu.ozu.mapp.client.handlers.World;
import edu.ozu.mapp.utils.*;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("Duplicates")
public class Greedy extends Agent
{
    public Greedy()
    {
        this("Greedy Agent 1", "GREEDY1", new Point(2, 0), new Point(2, 10));
        isHeadless = false;
    }

    public Greedy(String agentName, String agentID, Point start, Point dest)
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

        if (current_tokens > 0) {
            // give best bid, which is the current available path
            return new Action(this, ActionType.OFFER, getBroadcastArray());
        } else {
            // give second best bid or accept opponent's offer
            // or accept
            // TODO BETTER HISTORY STRUCTURE - GET LAST BID BETTER
            BidStruct lastBid = null;
            for (String agentID : history.keySet()) { // get last bid
                // AgentID is in format "agent:ID"
                if (agentID.equals("agent:"+AGENT_ID))
                    continue;
                // get the last bid
                ArrayList<BidStruct> opponentBids = history.get("agent:"+agentID);
                lastBid = opponentBids.get(opponentBids.size()-1);
            }
            if (lastBid != null) {
                // if last bid of opponent does not collide with my path
                // accept it
                String[] broadcast = getBroadcastArray();
                String[] bid_path = lastBid.path.replaceAll("([\\[\\]]*)", "").split(",");
                // if last bid does not collides
                boolean willAccept = true;
                for (int i = 0; i < broadcast.length; i++) { // TODO PROVIDE BUILT IN FUNCTION
                    if (broadcast[i].equals(bid_path[i]))
                    {   // has collision
                        willAccept = false;
                        break;
                    }
                }
                // accept if no collision
                if (willAccept)
                {
                    return new Action(this, ActionType.ACCEPT);
                }
            }
            return new Action(this, ActionType.OFFER, bids.get(0));
        }
    }

    public static void main(String[] args)
    {
        new AgentClient(args, new Greedy());
    }
}
