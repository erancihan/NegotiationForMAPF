package edu.ozu.mapp.utils;

import edu.ozu.mapp.agent.Agent;
import edu.ozu.mapp.agent.client.handlers.Negotiation;
import edu.ozu.mapp.agent.client.handlers.WorldHandler;
import edu.ozu.mapp.agent.client.models.Contract;

public class Action
{
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Action.class);

    public ActionType type;
    public Contract bid;
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

        switch(type) {
            case OFFER:
                this.bid = makeOffer(agent, type, bid);
                break;
            case ACCEPT:
                this.bid = Negotiation.getContract(agent);
                break;
            default:
                break;
        }
        this.type = type;

        logger.debug("taking action " + this.type + " - " + this.bid);
    }

    private Contract makeOffer(Agent agent, ActionType type, String bid)
    {
        int owned_tokens = WorldHandler.getTokenBalance(agent.WORLD_ID, agent.AGENT_ID);   // get own token balance
        logger.debug("owned tokens: " + owned_tokens);

        Contract contract = Negotiation.getContract(agent);

        int own_token_offer = Integer.parseInt(contract.getTokenOf(agent)); // get own bid token count

        boolean will_increase_tokens = false;
        // if current bid is in agents bid history
        if (agent.getOwnBidHistory().contains(bid)) {
            will_increase_tokens = true;
        }

        // if current bid is the same as broadcast, demand more tokens
        if (agent.getBroadcast().equals(bid)) {
            will_increase_tokens = true;
        }
        logger.debug("broadcast   : " + agent.getBroadcast());
        logger.debug("bid         : " + bid);
        logger.debug("token offer : " + own_token_offer);
        if (will_increase_tokens) {
            own_token_offer += 1;

            if (owned_tokens < own_token_offer) {
                logger.error("cannot demand same path without tokens: " + owned_tokens + " < " + own_token_offer);
                System.exit(1);
            }
        }
        logger.debug("token offer : " + own_token_offer);

        contract.set(agent, bid, own_token_offer);

        return contract;
    }

    public String toWSMSGString() {
        return agent.AGENT_ID + "-" + type.toString().toLowerCase();
    }

    @Override
    public String toString() {
        return String.format("Action{type: %6s, bid: %s, agent: %s}", type, bid, agent.AGENT_ID);
    }
}
