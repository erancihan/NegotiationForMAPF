package edu.ozu.mapp.utils;

import edu.ozu.mapp.agent.Agent;
import edu.ozu.mapp.agent.client.models.Contract;

public class Action
{
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Action.class);

    public ActionType type;
    public Contract bid;
    private Agent agent;
    private String _bid;

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
        this.type  = type;
        this._bid  = bid;
    }

    public Action(Agent agent, ActionType type, Bid bid)
    {
        this(agent, type, bid.path.toStringArray());
    }

    public String toWSMSGString() {
        return agent.AGENT_ID + "-" + type.toString().toLowerCase();
    }

    @Override
    public String toString() {
        return String.format("Action{type: %6s, bid: %s, agent: %s}", type, bid, agent.AGENT_ID);
    }

    @SuppressWarnings("DuplicatedCode")
    public boolean validate()
    {
        int owned_tokens = agent.GetCurrentTokens();   // get own token balance
        logger.debug("owned tokens: " + owned_tokens);

        Contract contract = agent.GetContract();
        int own_token_offer = Integer.parseInt(contract.getTokenCountOf(agent)); // get own bid token count

        boolean will_increase_tokens = false;
        // if current bid is in agents bid history
        if (agent.getOwnBidHistory().contains(_bid)) {
            will_increase_tokens = true;
        }

        // if current bid is the same as broadcast, demand more tokens
        if (agent.getBroadcast().equals(_bid)) {
            will_increase_tokens = true;
        }

        if (will_increase_tokens) {
            own_token_offer += 1;

            if (owned_tokens <= 0 || owned_tokens < own_token_offer) {
                logger.debug(agent.AGENT_ID + " invalid action: " + owned_tokens + " < " + own_token_offer);
                return false;
            }
        }

        return true;
    }

    @SuppressWarnings("DuplicatedCode")
    public void take()
    {
        if (type == ActionType.ACCEPT)
        {
            this.bid = agent.GetContract();

            logger.debug("taking action " + this.type + " - " + this.bid);
            return;
        }

        int owned_tokens = agent.GetCurrentTokens();   // get own token balance
        logger.debug("owned tokens: " + owned_tokens);

        Contract contract = agent.GetContract();

        int own_token_offer = Integer.parseInt(contract.getTokenCountOf(agent)); // get own bid token count

        boolean will_increase_tokens = false;
        // if current bid is in agents bid history
        if (agent.getOwnBidHistory().contains(_bid)) {
            will_increase_tokens = true;
        }

        // if current bid is the same as broadcast, demand more tokens
        if (agent.getBroadcast().equals(_bid)) {
            will_increase_tokens = true;
        }
        logger.debug(agent.AGENT_ID + " broadcast   : " + agent.getBroadcast());
        logger.debug(agent.AGENT_ID + " bid         : " + _bid);
        logger.debug(agent.AGENT_ID + " token offer : " + own_token_offer);
        if (will_increase_tokens) {
            own_token_offer += 1;

            if (owned_tokens < own_token_offer) {
                logger.error(agent.AGENT_ID + " cannot demand same path without tokens: " + owned_tokens + " < " + own_token_offer);
                System.exit(1);
            }
        }
        logger.debug("token offer : " + own_token_offer);

        contract.set(agent, _bid, own_token_offer);
        this.bid = contract;

        logger.debug(agent.AGENT_ID + " taking action " + this.type + " - " + this.bid);
    }
}
