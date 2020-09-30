package mappagent.sample;

import edu.ozu.mapp.agent.Agent;
import edu.ozu.mapp.agent.MAPPAgent;
import edu.ozu.mapp.agent.client.AgentClient;
import edu.ozu.mapp.agent.client.helpers.WorldHandler;
import edu.ozu.mapp.agent.client.models.Contract;
import edu.ozu.mapp.utils.*;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings("Duplicates")
@MAPPAgent
public class Conceder extends Agent {
    private String current_opponent;
    private List<Bid> bid_space = new ArrayList<>();
    private Iterator<Bid> bid_space_iterator;

    public Conceder()
    {
        this("Conceder 1", "CONCEDER1", new Point(2, 0), new Point(2, 10));
        isHeadless = false;
    }

    public Conceder(String agentName, String agentID, Point start, Point dest)
    {
        super(agentName, agentID, start, dest);
    }

    @Override
    public void init()
    {
        super.init();

        // get initial planned path and remember it forever
    }

    @Override
    public void PreNegotiation(State state)
    {
        // Get Current Bid Space
        bid_space = GetCurrentBidSpace();
        bid_space_iterator = bid_space.iterator();
//        System.out.println(Arrays.toString(bid_space.toArray(new Bid[0])));
//        System.exit(1);

        // since this is a Bi-lateral negotiation, there should be only
        // two participant in the negotiation
        Assert.isTrue(state.agents.length == 2, "There are more agents than expected");

        // filter matching out
        current_opponent = Arrays.stream(state.agents).filter(a -> !a.equals(AGENT_ID)).collect(Collectors.toList()).get(0);
    }

    @Override
    public Action onMakeAction()
    {
        int current_tokens = WorldHandler.getTokenBalance(WORLD_ID, AGENT_ID);

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
        } else {
            if (current_tokens == 0)
            {   // i can do nothing but accept
                return new Action(this, ActionType.ACCEPT);
            }

            if (own_last_bid == null)
            {   // I haven't made an offer before
                String[] path_to_bid = GetOwnBroadcastPath();
                return new Action(this, ActionType.OFFER, path_to_bid);
            }

            // opponent has bid
            // check if it is viable for us
            Path opponent_path = new Path(last_opponent_bid.Ox);
            Path own_last_path = new Path(own_last_bid.Ox);
            if (opponent_path.HasConflictWith(own_last_path))
            {   // then propose the next possible option from
                if (bid_space_iterator.hasNext()) {
                    Bid bid = bid_space_iterator.next();
                    return new Action(this, ActionType.OFFER, bid);
                }
            } else {
                // there are no conflicts between my last bid & opponent's last
                // i can accept
                // todo I may recalculate a different path since i accepted
                // opponents decision ...
                return new Action(this, ActionType.ACCEPT);
            }

            return new Action(this, ActionType.OFFER, bid_space.get(0));
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
