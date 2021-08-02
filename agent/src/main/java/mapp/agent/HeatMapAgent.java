package mapp.agent;

import edu.ozu.mapp.agent.Agent;
import edu.ozu.mapp.agent.MAPPAgent;
import edu.ozu.mapp.agent.client.models.Contract;
import edu.ozu.mapp.system.Broadcast;
import edu.ozu.mapp.system.FoV;
import edu.ozu.mapp.utils.*;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

@MAPPAgent
public class HeatMapAgent extends Agent
{
    public HeatMapAgent(
            String agent_name,
            String agent_id,
            Point start,
            Point dest,
            int initial_tokens
    )
    {
        super(agent_name, agent_id, start, dest, initial_tokens);
    }

    // bid space
    private List<Bid> bid_space = null;
    private Iterator<Bid> bid_space_iterator = null;

    // heat map
    private HashMap<String, Double> HeatMap = null;

    @Override
    public double UtilityFunction(SearchInfo search)
    {
        // ensure heat map
        assert HeatMap != null;

        // Offset bid by how far is the last point to destination.
        // This should affect the utility the least.
        // Multiply the value with 1E-5 to decrease it's offset effect
        double offset = search.Path.getLast().ManhattanDistTo(DEST) * 1E-5;

        // apply heat map weights
        double weight = 0;
        for (Point point : search.Path)
        {   // sum the weights of cells on the map
            weight += HeatMap.getOrDefault(point.key, 0.0);
        }
        // adjust the effect of the weights on the thousandths
        //  0.001
        weight = weight * 1E-3;

        return
        (
            (
                1
                - ((search.PathSize - search.MinPathSize)
                    /
                    (search.MaxPathSize - search.MinPathSize)
                )
            )
            - weight
            - offset
        );
    }

    @Override
    public void PreNegotiation(State state)
    {
        // set bounds
        bound_r = worldOverseerReference.getWidth();
        bound_b = worldOverseerReference.getHeight();

        // generate heat map
        this.HeatMap = new HashMap<>();
        generate_heatmap();

        // get bid space
        bid_space = GetCurrentBidSpace();
        bid_space_iterator = bid_space.iterator();
    }

    int bound_l = 0;    int bound_r;
    int bound_t = 0;    int bound_b;
    private void generate_heatmap()
    {
        // fetch Field of View
        FoV fov = GetFieldOfView();

        // fill heat map information
        for (Broadcast broadcast : fov.broadcasts)
        {
            // skip own broadcast
            if (broadcast.agent_name.equals(this.AGENT_NAME)) continue;

            // get current location of agents
            // index 0 of a broadcast is agents current location
            Point location = broadcast.locations.get(0).location;
            // add weights to heat map
            //| 1 | 2 | 1 |
            //| 2 | 3 | 2 |
            //| 1 | 2 | 1 |
            int[] ws = new int[]{
                1,  2,  1,
                2,  3,  2,
                1,  2,  1
            };
            for (int i = 0; i < 9; i++)
            {
                int x = location.x + ((i % 3) - 1);
                int y = location.y + ((i / 3) - 1);

                if (x < bound_l || bound_r <= x) continue;  // x : [bound_l, bound_r) | i.e. [0, 16) -> x : 0, 1, ... 15
                if (y < bound_t || bound_b <= y) continue;  // y : [bound_t, bound_b) | i.e. [0, 16) -> y : 0, 1, ... 15

                int w = ws[i];
                // add weight to point increasingly
                HeatMap.put(
                    String.format("%d-%d", x, y), // key
                    HeatMap.getOrDefault(String.format("%d-%d", x, y), 0.0) + w // value
                );
            }
        }
    }

    @SuppressWarnings("DuplicatedCode")
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

    @SuppressWarnings("DuplicatedCode")
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

    @SuppressWarnings("DuplicatedCode")
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
    }}
