package mapp.agent;

import edu.ozu.mapp.agent.Agent;
import edu.ozu.mapp.agent.MAPPAgent;
import edu.ozu.mapp.agent.client.models.Contract;
import edu.ozu.mapp.system.Broadcast;
import edu.ozu.mapp.system.FoV;
import edu.ozu.mapp.utils.*;

import java.util.*;

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
    private HashSet<Object> previous_bids = null;

    @Override
    public void PreNegotiation(State state)
    {
        // set bounds
        bound_r = worldOverseerReference.getWidth();
        bound_b = worldOverseerReference.getHeight();

        generate_heatmap();

        // get bid space
        List<Bid> current_bid_space = GetCurrentBidSpace();   // returns a sorted bid space

        // get bids list
        PriorityQueue<Bid> bids = new PriorityQueue<>(Comparator.comparingDouble(this::get_weight));
        bids.addAll(current_bid_space);

        bid_space = new ArrayList<>();
        while (!bids.isEmpty()) { bid_space.add(bids.poll()); }

        previous_bids = new HashSet<>();
    }

    private double get_weight(Bid bid)
    {
        double weight = 0;

        for (Point point : bid.path)
        {   // add weights from heat map
            // each step taken is also +1 weight
            weight += 1 + HEAT_MAP.getOrDefault(point.key, 0.0);
        }

        return weight;
    }

    int bound_l = 0;    int bound_r;
    int bound_t = 0;    int bound_b;
    private HashMap<String, Double> HEAT_MAP = null;    // heat map
    private void generate_heatmap()
    {
        this.HEAT_MAP = new HashMap<>();

        // fetch Field of View
        FoV fov = GetFieldOfView();

        int dims = Globals.FIELD_OF_VIEW_SIZE;
        int[] heat_map_weights = new int[dims * dims];
        for (int i = 0; i < dims; i++)
        {   // rows
            for (int j = 0; j < dims; j++)
            {   // cols
                heat_map_weights[((i * dims) + j)] =
                        (dims - (Math.abs(i - (dims / 2)) + Math.abs(j - (dims / 2))));
            }
        }

        // fill heat map information
        for (Broadcast broadcast : fov.broadcasts)
        {
            // skip own broadcast
            if (broadcast.agent_name.equals(this.AGENT_NAME)) continue;

            // get current location of agents
            // index 0 of a broadcast is agents current location
            Point location = broadcast.locations.get(0).location;

            // dynamic heat map window definition & application based on
            //   Field of View
            // apply heat map
            for (int i = 0; i < (dims * dims); i++) {
                int x = location.x + ((i % dims) - (dims / 2));
                int y = location.y + ((i / dims) - (dims / 2));

                if (x < bound_l || bound_r <= x) continue;  // x : [bound_l, bound_r) | i.e. [0, 16) -> x : 0, 1, ... 15
                if (y < bound_t || bound_b <= y) continue;  // y : [bound_t, bound_b) | i.e. [0, 16) -> y : 0, 1, ... 15

                int w = heat_map_weights[i];
                // add weight to point increasingly
                HEAT_MAP.put(
                    String.format("%d-%d", x, y), // key
                    HEAT_MAP.getOrDefault(String.format("%d-%d", x, y), 0.0) + w // value
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
