package edu.ozu.mapp.utils;

import edu.ozu.mapp.agent.Agent;
import edu.ozu.mapp.agent.client.handlers.Negotiation;
import edu.ozu.mapp.agent.client.handlers.World;

public class Action
{
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Action.class);

    public ActionType type;
    public String bid;
    private Agent agent;

    public Action(Agent agent, ActionType type)
    {
        this(agent, type, "");
    }

    public Action(Agent agent, ActionType type, String[] bid)
    {
        this(agent, type, Utils.toString(bid, ","));
    }

    private Action(Agent agent, ActionType type, String bid)
    {   // only invoked when making a bid
        this.agent = agent;

        if (type == ActionType.OFFER)
        {
            if(bid.length() == 0)
            {
                logger.error("Bid cannot be empty for Action Type " + type.toString());
                System.exit(1);
            }
            int agent_tokens = World.getTokenBalance(agent.WORLD_ID, agent.AGENT_ID);
            String[] current_bid_data = Negotiation.getActiveBid(agent.WORLD_ID, agent.AGENT_ID).split(":");
            if (current_bid_data.length != 2) {
                logger.error("something went wrong");
                System.exit(1);
            }
            int current_token_offer = Integer.parseInt(current_bid_data[1]);
            String current_bid_path = current_bid_data[0];

            // if previous bid is the same as current bid,
            if (current_bid_path.equals(bid))
                current_token_offer++;

            // if current bid is the same as broadcast, demand more tokens
            if (agent.getBroadcast().equals(bid))
                current_token_offer++;

            if (agent_tokens < current_token_offer) {
                logger.error("cannot demand same path without tokens: " + agent_tokens + " < " + current_token_offer);
                System.exit(1);
            }
            this.bid = bid + ":" + current_token_offer;
        }
        if (type == ActionType.ACCEPT)
        {
            this.bid = Negotiation.getActiveBid(agent.WORLD_ID, agent.AGENT_ID);
        }
        this.type = type;

        logger.debug("taking action " + this.type + " - " + this.bid);
    }

    @Override
    public String toString() {
        return agent.AGENT_ID + "-" + type.toString().toLowerCase() + "-" + bid;
    }
}
