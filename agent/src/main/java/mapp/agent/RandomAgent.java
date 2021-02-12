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

    private List<Bid> bid_space = new ArrayList<>();
    private Iterator<Bid> bid_space_iterator;

    public RandomAgent(String agentName, String agentID, Point start, Point dest, int initial_tokes) {
        super(agentName, agentID, start, dest, initial_tokes);

        random = new Random();
    }

    @Override
    public void PreNegotiation(State state)
    {
        bid_space = GetCurrentBidSpace();
        bid_space_iterator = bid_space.iterator();
    }

    @Override
    public Action onMakeAction(Contract contract) {
        if (contract.Ox.isEmpty())
        {   // i am the first one bidding, i have to make an offer
            return new Action(this, ActionType.OFFER, bid_space_iterator.next());
        }

        if (random.nextDouble() < 0.5)
        {   // accept
            return new Action(this, ActionType.ACCEPT);
        }
        else
        {   // counter offer
            Bid next_bid = bid_space_iterator.next();

            if (getOwnBidHistory().contains(next_bid.path.string()))
            {   // insisting
                // check if i have any tokens left
                if (contract.GetTokenCountOf(this) == current_tokens)
                {   // i dont have anymore tokens, i cant insist, just accept
                    return new Action(this, ActionType.ACCEPT);
                }
            }

            return new Action(this, ActionType.OFFER, next_bid);
        }
    }
}
