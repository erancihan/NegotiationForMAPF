package mapp.agent;

import edu.ozu.mapp.agent.Agent;
import edu.ozu.mapp.agent.MAPPAgent;
import edu.ozu.mapp.agent.client.models.Contract;
import edu.ozu.mapp.utils.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

@MAPPAgent
public class RandomAgent extends Agent {
    public Random random;

    private Iterator<Bid> bid_space_iterator;

    public RandomAgent(String agentName, String agentID, Point start, Point dest, int initial_tokes) {
        super(agentName, agentID, start, dest, initial_tokes);

        random = new Random();
    }

    @Override
    public void PreNegotiation(State state)
    {
        bid_space_iterator = GetCurrentBidSpace().iterator();
    }

    @Override
    public Action onMakeAction(Contract contract) {
        if (contract.Ox.isEmpty())
        {   // i am the first one bidding, i have to make an offer
            Action action;
            do {
                // get a valid first action
                action = new Action(this, ActionType.OFFER, bid_space_iterator.next());
            } while (!action.validate());
            return action;
        }

        if (random.nextDouble() < 0.5)
        {   // accept
            return new Action(this, ActionType.ACCEPT);
        }
        else
        {   // counter offer
            Bid next_bid = bid_space_iterator.next();

            Action action = new Action(this, ActionType.OFFER, next_bid);

            if (!action.validate())
            {   // insisting
                // if action is invalid, send accept
                // it is possible to loop through though...
                return new Action(this, ActionType.ACCEPT);
            }

            return action;
        }
    }
}
