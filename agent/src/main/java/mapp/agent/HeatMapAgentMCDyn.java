package mapp.agent;

import edu.ozu.mapp.agent.Agent;
import edu.ozu.mapp.agent.MAPPAgent;
import edu.ozu.mapp.agent.client.helpers.ConflictCheck;
import edu.ozu.mapp.agent.client.helpers.ConflictInfo;
import edu.ozu.mapp.agent.client.models.Contract;
import edu.ozu.mapp.dataTypes.Constraint;
import edu.ozu.mapp.system.Broadcast;
import edu.ozu.mapp.system.fov.FoV;
import edu.ozu.mapp.utils.*;
import edu.ozu.mapp.utils.bid.BidSpace;
import edu.ozu.mapp.utils.path.Path;

import java.util.*;

@MAPPAgent
public class HeatMapAgentMCDyn extends Agent
{
    private int CAP = 999;

    public HeatMapAgentMCDyn(String agent_name, String agent_id, Point start, Point dest, int initial_tokens)
    {
        super(agent_name, agent_id, start, dest, initial_tokens);
    }

    // bid space
    private List<Bid> bid_space = null;
    private Iterator<Bid> bid_space_iterator = null;
    private HashSet<Object> previous_bids = null;

    int bound_l = 0;    int bound_r;
    int bound_t = 0;    int bound_b;
    private ArrayList<HashMap<String, Double>> HEAT_MAPS = null;
    private HashMap<String, Double> OBSTACLES = null;

    @Override
    public ArrayList<Constraint> prepareConstraints(ArrayList<Constraint> constraints)
    {
        return super.prepareConstraints(constraints);
    }

    @SuppressWarnings("DuplicatedCode")
    @Override
    public void OnMove(JSONAgent response) {
        Iterator<Map.Entry<String, ArrayList<Constraint>>> iterator = memory.entrySet().iterator();
        while (iterator.hasNext()) {
            ArrayList<Constraint> constraints = iterator.next().getValue();

            constraints.removeIf(constraint -> constraint.at_t < this.time);

            if (constraints.size() == 0) {
                iterator.remove();
            }
        }

        MAX_COMMITMENT_SIZE = Integer.MAX_VALUE;
    }

    @SuppressWarnings("DuplicatedCode")
    @Override
    public void PostNegotiation(Contract contract) {
        // get contract and register promise

        if (contract.x.equals(AGENT_ID)) {
            // I won, nothing to do here
            return;
        }

        // remember whom we promised, what we promised
        // register entire Ox as promised as well

        Point[] Ox = contract.GetOx();
        ArrayList<Constraint> constraints = new ArrayList<>();

        for (int i = 0; i < Ox.length && i <= MAX_COMMITMENT_SIZE; i++) {
            constraints.add(new Constraint(contract.x, Ox[i], this.time + i));
        }

        memory.put(contract.x, constraints);
    }
    private HashMap<String, ArrayList<Constraint>> memory = new HashMap<>();
    private int MAX_COMMITMENT_SIZE = Integer.MAX_VALUE;

    @SuppressWarnings("DuplicatedCode")
    @Override
    public void PreNegotiation(State state)
    {
        HashSet<String> participants = new HashSet<>(Arrays.asList(state.agents));

        ArrayList<Constraint> constraints = new ArrayList<>();

        // fetch Field of View
        FoV fov = GetFieldOfView();
        for (Broadcast broadcast : fov.broadcasts) {
            // skip if own ID
            if (broadcast.agent_name.equals(AGENT_ID)) {
                continue;
            }
            if (participants.contains(broadcast.agent_name)) {
                // opponent data
                ConflictInfo ci = new ConflictCheck().check(
                        this.GetOwnBroadcastPath(),
                        broadcast.getPathStringArray()
                );

                assert ci.hasConflict;

                if (ci.type.equals(ConflictCheck.ConflictType.VertexConflict)) {
                    MAX_COMMITMENT_SIZE = ci.index;
                }
                if (ci.type.equals(ConflictCheck.ConflictType.SwapConflict)) {
                    MAX_COMMITMENT_SIZE = ci.index + 1;
                }
            }

            ArrayList<Constraint> cs = memory.get(broadcast.agent_name);
            if (cs == null) {
                // skip if no memory of item
                continue;
            }

            // check if broadcast starts with the promised locations
            boolean should_constraint = true;
            for (int i = 0; i < broadcast.locations.size(); i++) {
                if (i >= cs.size()) {
                    // memory is shorter than broadcast
                    continue;   // OOB
                }

                Constraint _c_a = cs.get(i);
                Constraint _c_b = broadcast.locations.get(i);

                if (_c_b.equals(_c_a)) {
                    continue;
                }

                // agent broadcast does not match agent's promise
                //   Ox constraint memoization is invalid
                should_constraint = false;
                // forget the locations as well
                memory.remove(broadcast.agent_name);
            }

            if (!should_constraint) {
                continue;
            }

            // I promised this guy
            constraints.addAll(cs);
        }

        HashMap<String, ArrayList<String>> constraintHashMap = constraints2hashmap(constraints);

        // set bounds
        bound_r = worldOverseerReference.getWidth();
        bound_b = worldOverseerReference.getHeight();

        // BEGIN: HEATMAP/HEIGHTMAP GEN
        HEAT_MAPS = new ArrayList<>();
        OBSTACLES = new HashMap<>();

        int dims = Globals.FIELD_OF_VIEW_SIZE;

        // GENERATE HEAT MAP WINDOW
        // BEGIN
        double[] heat_map_weights = new double[dims * dims];
        int center = (dims / 2) * (dims + 1);
        for (int i = 0; i < dims; i++)
        {   // rows
            for (int j = 0; j < dims; j++)
            {   // cols
                int index = ((i * dims) + j);

                heat_map_weights[index] =
                    (index == center)
                        ? 1         // Agent is here
                        : Math.max( // Normalized height effect on location
                            0.0,
                            (
                                (double) (((dims / 2) + 1) - (Math.abs(i - (dims / 2)) + Math.abs(j - (dims / 2))))
                                /
                                (double) ((dims / 2) + 1)
                            )
                        )
                    ;
            }
        }
        // END

        // FILL HEAT MAP INFORMATION
        // BEGIN

        // BEGIN : get obstacles
        for (Point obstacle : fov.obstacles)
        {
            // `fov.obstacle` contains:
            //  - agents that have reached their destinations, depending on LEAVE behaviour
            //  - defined environment obstacles, if there are any
            OBSTACLES.put(obstacle.key, (double) CAP);
        }
        // END : get obstacles

        for (Broadcast broadcast : fov.broadcasts)
        {
            if (broadcast.agent_name.equals(this.AGENT_NAME)) {
                continue;   // skip own broadcast
            }
            if (participants.contains(broadcast.agent_name)) {
                continue;   // skip if participant of the negotiation
            }

            // iter through broadcast locations
            ArrayList<Constraint> locations = broadcast.locations;
            for (int i = 0; i < locations.size(); i++)
            {
                // get current location of agents
                Point location = locations.get(i).location;

                if (HEAT_MAPS.size() <= i) { HEAT_MAPS.add(new HashMap<>()); }
                HashMap<String, Double> heat_map = HEAT_MAPS.get(i);

                // add location weights
                // ---
                // Agent that is at the center of the `heat_map_weights` will be considered
                //   as obstacle if that value is CAP. In this configuration, CAP is `999`,
                //   and since this configuration also aims to _not_ have agent's as definite
                //   obstacle, `center` point of `heat_map_weights` evaluates to 1, as it is
                //   the normalized value. TODO: TBD
                // ---
                for (int j = 0; j < heat_map_weights.length; j++)
                {
                    int x = location.x + ((j % dims) - (dims / 2));
                    int y = location.y + ((j / dims) - (dims / 2));

                    if (x < bound_l || bound_r <= x) continue;  // x : [bound_l, bound_r) | i.e. [0, 16) -> x : 0, 1, ... 15
                    if (y < bound_t || bound_b <= y) continue;  // y : [bound_t, bound_b) | i.e. [0, 16) -> y : 0, 1, ... 15

                    double weight =
                            heat_map_weights[j] +
                            OBSTACLES.getOrDefault(String.format("%d-%d", x, y), 0.0);
                    heat_map.put(
                            String.format("%d-%d", x, y), // key
                            heat_map.getOrDefault(String.format("%d-%d", x, y), 0.0) + weight // value
                    );
                }
                HEAT_MAPS.set(i, heat_map);
                // END : WEIGHT APPLY
            }
            // END : BROADCAST LOCATIONS ITER
        }
        // END
        // END : HEATMAP/HEIGHTMAP GEN

        // generate bid space
        BidSpace space = new BidSpace();
        space.init(POS, DEST, Globals.FIELD_OF_VIEW_SIZE, constraintHashMap, this.dimensions, time);
        space.prepare();

        // generate default bid space ordering
        // BEGIN
        double max_l = Double.MIN_VALUE;
        double min_l = Double.MAX_VALUE;
        double max_w = Double.MIN_VALUE;
        double min_w = Double.MAX_VALUE;

        List<Path> paths = new ArrayList<>();
        int poll_c = 0;
        while (paths.size() < Globals.MAX_BID_SPACE_POOL_SIZE)
        {
            Path next = space.next();
            if (next == null) break;
            if (next.size() == 0) break;
            if ((poll_c - paths.size()) == Globals.MAX_BID_SPACE_POLL_COUNT) break; // check extra poll count
            poll_c++;

            // add rest to path
            List<String> rest = new AStar().calculate(next.getLast(), DEST, this.dimensions);
            for (int j = 1; j < rest.size(); j++) {
                next.add(new Point(rest.get(j), "-"));
            }

            double w = get_weight(next);
            if (w >= CAP) { continue; }

            next.properties.put("weight", String.valueOf(w));
            double l = next.size();
            if (l > max_l) max_l = l;
            if (l < min_l) min_l = l;
            if (w > max_w) max_w = w;
            if (w < min_w) min_w = w;

            paths.add(next);
            this.file_logger.LogBid(this.AGENT_ID, this.WORLD_ID, state.session_id, next);
        }
        this.file_logger.LogBid(this.AGENT_ID, this.WORLD_ID, state.session_id, null);
        // END

        PriorityQueue<Bid> bids = new PriorityQueue<>(Comparator.reverseOrder());
        for (Path path : paths)
        {
            bids.add(new Bid(AGENT_ID, path, utility_function(max_w, min_w, max_l, min_l, path)));
        }

        bid_space = new ArrayList<>();
        while (!bids.isEmpty()) {
            Bid bid = bids.poll();
            bid_space.add(bid);
            this.file_logger.LogBidSorted(this.AGENT_ID, this.WORLD_ID, state.session_id, bid);
        }
        this.file_logger.LogBidSorted(this.AGENT_ID, this.WORLD_ID, state.session_id, null);

        bid_space_iterator = bid_space.iterator();
        previous_bids = new HashSet<>();
    }

    private double utility_function(double max_w, double min_w, double max_l, double min_l, Path path)
    {
        double offset = (1 - ((max_l - path.size()) / (max_l - min_l))) * 1E-6; // offset by normalized path length

        return (1 - (Double.parseDouble(path.properties.get("weight")) - min_w) / (max_w - min_w)) - offset;
    }

    private double get_weight(Path path) {
        double w = 0.0;
        for (int i = 0; i < path.size(); i++)
        {
            Point point = path.get(i);
            w = w
                + 1.0                                       // add move
                + OBSTACLES.getOrDefault(point.key, 0.0)    //  INF if obstacle
                + ((i < HEAT_MAPS.size())                   // check if there is info of agent locs
                    ? HEAT_MAPS.get(i).getOrDefault(point.key, 0.0)
                    : 0.0
                )
            ;
        }

        return w;
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
