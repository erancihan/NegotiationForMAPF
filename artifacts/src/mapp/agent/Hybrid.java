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
        int bid_token_count = contract.GetTokenCountOf(this);

        int token_rate = (this.current_tokens - bid_token_count) / this.initial_tokens;
        int token_path = GetMyRemainingPathLength() / this.initial_path;

        int rate = token_rate / token_path;

        // todo AC-Next

        if (rate < 1)
        {   // CONCEDE
            return run_conceder(contract);
        }
        else
        {   // GREED
            return run_greedy(contract);
        }
    }

    private Action run_conceder(Contract contract)
    {
        int current_tokens = GetCurrentTokens();

        // get opponent's bid
        Contract last_opponent_bid = history.GetLastOpponentBid(current_opponent);
        Contract last_own_bid = history.GetLastOwnBid();

        if (contract.Ox.isEmpty())
        {
            // contract is empty
            // I am the one doing the first bid of negotiation
            // propose current path by default, as it is the
            // current best possible bid
            String[] path_to_bid = GetOwnBroadcastPath();
            return new Action(this, ActionType.OFFER, path_to_bid);
        }

        if (current_tokens == 0)
        {   // i can do nothing but accept
            return new Action(this, ActionType.ACCEPT);
        }

        // opponent has bid
        // check if it is viable for us
        Path opponent_path;
        if (last_opponent_bid == null)
            opponent_path = GetOpponentCurrentlyBroadcastedPath();
        else
            opponent_path = new Path(last_opponent_bid.Ox);

        Path own_last_path;
        if (last_own_bid == null)
            own_last_path = new Path(GetOwnBroadcastPath());
        else
            own_last_path = new Path(last_own_bid.Ox);

        if (opponent_path.HasConflictWith(own_last_path))
        {   // opponent said they will take a path that conflicts with own
            // are they insisting on it?
            int opponent_offered_tokens =  contract.GetOpponentTokenProposal(this);
            if (opponent_offered_tokens > 0)
                return new Action(this, ActionType.ACCEPT);

            // then propose the next possible option from
            if (bid_space_iterator.hasNext()) {
                Bid bid = bid_space_iterator.next();
                return new Action(this, ActionType.OFFER, bid);
            }
        } else {
            // there are no conflicts between my last bid & opponent's last
            // i can accept
            return new Action(this, ActionType.ACCEPT);
        }

        return new Action(this, ActionType.OFFER, bid_space.get(0));
    }

    private Action run_greedy(Contract contract)
    {
        int current_tokens = Integer.parseInt(WorldOverseer.getInstance().GetAgentData(AGENT_ID)[1]);

        // get opponent's bid
        Contract last_opponent_bid = history.GetLastOpponentBid(current_opponent);
        Contract own_last_bid = history.GetLastOwnBid();

        if (last_opponent_bid == null)
        {
            // I am the one doing the first bid of negotiation
            // propose current path by default, as it is the
            // current best possible bid
            String[] path_to_bid = GetOwnBroadcastPath();
            return new Action(this, ActionType.OFFER, path_to_bid);
        }

        if (current_tokens == 0)
        {   // i can do nothing but accept
            return new Action(this, ActionType.ACCEPT);
        }

        if (own_last_bid == null)
        {   // I haven't made an offer before
            String[] path_to_bid = GetOwnBroadcastPath();
            return new Action(this, ActionType.OFFER, path_to_bid);
        }

        // opponent has bid, but i don't care
        // try to stay the course as long as possible
        int last_own_bid_tc = Integer.parseInt(own_last_bid.getTokenCountOf(this));
        if (last_own_bid_tc + 1 <= current_tokens)
        {
            String[] path_to_bid = GetOwnBroadcastPath();
            return new Action(this, ActionType.OFFER, path_to_bid);
        }

        // i cant insist anymore, give up
        return new Action(this, ActionType.ACCEPT);
    }
}
