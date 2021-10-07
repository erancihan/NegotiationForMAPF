package mapp.agent;

import edu.ozu.mapp.agent.Agent;
import edu.ozu.mapp.agent.MAPPAgent;
import edu.ozu.mapp.agent.client.models.Contract;
import edu.ozu.mapp.system.WorldOverseer;
import edu.ozu.mapp.utils.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@MAPPAgent
public class Hybrid extends Agent
{
    private List<Bid> bid_space = new ArrayList<>();
    private Iterator<Bid> bid_space_iterator;

    public Hybrid(String agentName, String agentID, Point start, Point dest, int initial_tokens)
    {
        super(agentName, agentID, start, dest, initial_tokens);
    }

    @Override
    public void PreNegotiation(State state)
    {
        // Get Current Bid Space
        bid_space = GetCurrentBidSpace();
        bid_space_iterator = bid_space.iterator();
    }

    @Override
    public Action onMakeAction(Contract contract)
    {
        double bid_token_count = contract.GetTokenCountOf(this);

        double token_rate = (this.current_tokens - bid_token_count) / this.initial_tokens;
        double path_rate = (double) GetMyRemainingPathLength() / (double) this.initial_path.size();

        double rate = token_rate / path_rate;

        if (rate < 1)
        {   // CONCEDE
            return run_concede(contract);
        }
        else
        {   // GREED
            return run_greedy(contract);
        }
    }

    public Action run_concede(Contract contract)
    {
        if (contract == null || contract.Ox.isEmpty())
        {   // i am the first bidder
            Action action;
            do {
                // get next valid action
                action = new Action(this, ActionType.OFFER, bid_space_iterator.next());
            } while (!action.validate());

            return action;
        }

        // there is a bid

        // can i calculate a path, with bid as constraint
        List<String> possible_path = calculatePath(POS, DEST, contract.Ox);
        if (possible_path == null)
        {   // i cannot accept this bid!
            Action action;
            do {
                action = new Action(this, ActionType.OFFER, bid_space_iterator.next());
            } while (!action.validate());

            return action;
        }

        int opponent_offered_tokens = contract.GetOpponentTokenProposal(this);
        if (opponent_offered_tokens > 0)
        {
            return new Action(this, ActionType.ACCEPT);
        }

        return new Action(this, ActionType.ACCEPT);
    }

    public Action run_greedy(Contract contract)
    {
        // insist on current broadcast
        Action insisting_action = new Action(this, ActionType.OFFER, GetOwnBroadcastPath());
        if (insisting_action.validate()) return insisting_action;

        if (bid_space_iterator != null && bid_space_iterator.hasNext())
        {
            Action action = new Action(this, ActionType.OFFER, bid_space_iterator.next());
            if (action.validate()) return action;
        }

        return new Action(this, ActionType.ACCEPT);
    }
}
