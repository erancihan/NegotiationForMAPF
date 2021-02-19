package edu.ozu.mapp.agent;

import edu.ozu.mapp.agent.client.AgentHandler;
import edu.ozu.mapp.agent.client.helpers.FileLogger;
import edu.ozu.mapp.agent.client.models.Contract;
import edu.ozu.mapp.dataTypes.Constraint;
import edu.ozu.mapp.keys.AgentKeys;
import edu.ozu.mapp.keys.KeyHandler;
import edu.ozu.mapp.system.NegotiationOverseer;
import edu.ozu.mapp.system.WorldOverseer;
import edu.ozu.mapp.utils.*;
import edu.ozu.mapp.utils.bid.BidSpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.PublicKey;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class Agent {
    public static final Logger logger = LoggerFactory.getLogger(Agent.class);
    private FileLogger file_logger;

    public String AGENT_NAME, AGENT_ID;
    public Point START, DEST;
    public History history;

    public int initial_tokens = Globals.INITIAL_TOKEN_BALANCE;
    public List<String> initial_path;

    public int current_tokens;
    public String current_opponent;

    public boolean isHeadless = false;

    public Point POS;
    public List<String> path;
    public int time = 0;

    public String WORLD_ID;
    public AgentKeys keys;
    private HashMap<String, String> conflict_locations;

    public int winC = 0;
    public int loseC = 0;

    /*
     * -1 | just started, nothing present
     *  0 | lost
     *  1 | won
     * */
    public int negotiation_result = -1;
    public String dimensions = "";

    public Function<SearchInfo, Double> UtilityFunction =
            (SearchInfo search) -> {
                // how far is the last point to destination
                double offset = 0;
                if (DEST != null) {
                    offset = search.Path.getLast().ManhattanDistTo(DEST) * 1E-5;
                }

                return (1 - ((search.PathSize - search.MinPathSize) / (search.MaxPathSize - search.MinPathSize)) - offset);
            };

    public ArrayList<Constraint> constraints;

    public Agent(String agentName, String agentID, Point start, Point dest)
    {
        this(agentName, agentID, start, dest, Globals.INITIAL_TOKEN_BALANCE);
    }

    public Agent(String agentName, String agentID, Point start, Point dest, int initial_tokens)
    {
        this.AGENT_NAME     = agentName;
        this.AGENT_ID       = agentID;
        this.START          = start;
        this.DEST           = dest;
        this.initial_tokens = initial_tokens;
        this.current_tokens = initial_tokens;

        this.constraints    = new ArrayList<>();
        this.conflict_locations = new HashMap<>();

        this.isHeadless = true; // unless client says so
        file_logger = FileLogger.getInstance();//.CreateAgentLogger(AGENT_ID);

        history = new History(AGENT_ID);
        // create and store agent keys
        keys = KeyHandler.getInstance().create(this);
    }

    public void init() { }

    public void PreNegotiation(State state) { }

    public void onReceiveState(State state) { }

    public abstract Action onMakeAction(Contract contract);

    public void OnAcceptLastBids(JSONNegotiationSession json) { }

    public void OnAcceptLastBids(Contract contract) { }

    public void PostNegotiation() { }

    public void OnMove(JSONAgent response) { }

    public final void run()
    {
        logger.info(AGENT_ID + " calculating path");
        path = calculatePath();
        logger.info(AGENT_ID + " calculation done");
        this.initial_path = new ArrayList<>(path);

        POS = new Point(path.get(0).split("-"));
        history = new History(AGENT_ID);
    }

    public List<String> calculatePath() {
        return calculatePath(START, DEST);
    }

    public final List<String> calculatePath(Point start, Point dest)
    {
        return calculatePath(start, dest, new ArrayList<>());
    }

    public final List<String> calculatePath(Point start, Point dest, ArrayList<Constraint> constraints)
    {
        HashMap<String, ArrayList<String>> _constraints = process_constraints(constraints);

        logger.debug(AGENT_ID + " | calculating A* {" + start + ", " + dest + ", " + _constraints + ", " + dimensions + ", " + time + "}");
        return new AStar().calculate(start, dest, _constraints, dimensions, time);
    }

    private HashMap<String, ArrayList<String>> process_constraints(ArrayList<Constraint> constraints)
    {
        HashMap<String, ArrayList<String>> _map = new HashMap<>();
        for (Constraint constraint : constraints) {
            if (!_map.containsKey(constraint.location.key)) {
                _map.put(constraint.location.key, new ArrayList<>());
            }
            _map.get(constraint.location.key).add(constraint.at_t == -1 ? "inf" : String.valueOf(constraint.at_t));
        }

        String[][] fov = WorldOverseer.getInstance().GetFoV(this.AGENT_ID);
        for (String[] fov_data : fov) {
            Point point = new Point(fov_data[1], "-");
            String loc_ = fov_data[2];

            if (loc_.equals("inf")) {
                _map.put(point.key, new ArrayList<>());
                _map.get(point.key).add(loc_);
            }
            /*
            else {
                Path path = new Path(loc_);
                for (int i = 0; i < path.size(); i++) {
                    Point p = path.get(i);
                    if (!constraints.containsKey(p.key)) {
                        constraints.put(p.key, new ArrayList<>());
                    }
                    constraints.get(p.key).add(String.valueOf(time + i));
                }
            }
            */
        }

        return _map;
    }

    public final List<Bid> GetBidSpace(Point From, Point To, int deadline)
    {
        BidSpace space = new BidSpace(
                From,
                To,
                deadline,
                process_constraints(this.constraints),
                this.dimensions.isEmpty() ? "0x0" : this.dimensions,
                time
        );

        double max = Double.MIN_VALUE;
        double min = Double.MAX_VALUE;
        List<Path> paths = new ArrayList<>();
        for (int i = 0; i < 100; i++)
        {
            Path next = space.next();
            if (next == null) break;

            double _max = next.size() + next.getLast().ManhattanDistTo(To);
            double _min = next.size() + next.getLast().ManhattanDistTo(To);

            if (_max > max) max = _max;
            if (_min < min) min = _min;

            paths.add(next);
        }

        PriorityQueue<Bid> bids = new PriorityQueue<>(Comparator.reverseOrder());
        for (Path path : paths)
        {
            bids.add(new Bid(
                    AGENT_ID,
                    path,
                    UtilityFunction.apply(new SearchInfo(max, min, path))
            ));
        }

        List<Bid> results = new ArrayList<>();
        while (!bids.isEmpty()) {
            results.add(bids.poll());
        }

        return results;
    }

    public final List<Bid> GetBidSpace(Point From, Point To)
    {
        return GetBidSpace(From, To, Globals.FIELD_OF_VIEW_SIZE);
    }

    public final List<Bid> GetCurrentBidSpace(int minimum_path_size)
    {
        return GetBidSpace(POS, DEST, minimum_path_size);
    }

    public final List<Bid> GetCurrentBidSpace(Point To)
    {
        return GetBidSpace(POS, To, Globals.FIELD_OF_VIEW_SIZE);
    }

    public final List<Bid> GetCurrentBidSpace()
    {
        // Set exit point of bid space search
        // as the last point of broadcast that is within FoV

        // Get current broadcast path
        String[] broadcast = GetOwnBroadcastPath();

        // find the last point that is within FoV
        Point to = new Point(broadcast[broadcast.length-1], "-");// null;
        /*
        for (int i = broadcast.length-1; i >= 0; i--) {
            // iterate in reverse
            Point point = new Point(broadcast[i], "-");
            if (point.x <= (POS.x + Globals.FIELD_OF_VIEW_SIZE / 2) && point.y <= (POS.y + Globals.FIELD_OF_VIEW_SIZE / 2)) {
                to = point;
                break;
            }
        }
        */

        if (to != null) {
            return GetBidSpace(POS, to, Globals.FIELD_OF_VIEW_SIZE);
        }

        logger.debug("selecting DEST for exit point");
        return GetBidSpace(POS, DEST, Globals.FIELD_OF_VIEW_SIZE);
    }

    //<editor-fold defaultstate="collapsed" desc="Get Broadcast">
    public final String getBroadcast()
    {
        return Utils.toString(GetOwnBroadcastPath(this.time), ",");
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Get Next Broadcast">
    public final String getNextBroadcast()
    {
        return Utils.toString(GetOwnBroadcastPath(this.time + 1), ",");
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Get Next Broadcast">
    public final String[] GetNextBroadcast()
    {
        return GetOwnBroadcastPath(this.time + 1);
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Get Broadcast Array">
    public final String[] GetOwnBroadcastPath()
    {
        return GetOwnBroadcastPath(time);
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Get Broadcast Array (int time) ">
    public final String[] GetOwnBroadcastPath(int time)
    {
        List<String> broadcast = new ArrayList<>();
        for (int i = 0; (i < Globals.BROADCAST_SIZE) && (i + time < path.size()); i++) {
            broadcast.add(path.get(i + time));
        }

        return broadcast.toArray(new String[0]);
    }
    //</editor-fold>

    public final Path GetOpponentCurrentlyBroadcastedPath()
    {
        return new Path(WorldOverseer.getInstance().GetBroadcast(current_opponent));
    }

    // TODO
    public final int GetMyCurrentTokenBalance()
    {
        return -1;
    }

    public final int GetMyRemainingPathLength()
    {
        return this.path.size() - time;
    }

    public final HashSet<String> getOwnBidHistory()
    {
        return history.get(AGENT_ID).stream().map(contract -> contract.Ox).collect(Collectors.toCollection(HashSet::new));
    }

    public final String Encrypt(String text)
    {
        return KeyHandler.encrypt(text, keys.GetPublicKey());
    }

    public final String Decrypt(String text)
    {
        return KeyHandler.decrypt(text, keys.GetPrivateKey(this));
    }

    public final PublicKey GetPubKey()
    {
        return keys.GetPublicKey();
    }

    public final void setWORLD_ID(String WORLD_ID)
    {
        this.WORLD_ID = WORLD_ID;
    }

    public final void SetConflictLocation(String session_id, String conflictLocation)
    {
        this.conflict_locations.put(session_id, conflictLocation);
    }

    public final String GetConflictLocation(String session_id)
    {
        return this.conflict_locations.get(session_id);
    }

    public final void OnContractUpdated(Contract contract)
    {
        history.put(AGENT_ID, contract);
    }

    public final String GetCurrentTokenC()
    {
        return String.valueOf(current_tokens);
    }

    public final int GetCurrentTokens()
    {
        return current_tokens;
    }

    public final Contract GetContract()
    {
        return NegotiationOverseer.getInstance().GetMyContract(this);
    }

    private AgentHandler handler_ref;
    public final void SetHandlerRef(AgentHandler handler)
    {
        handler_ref = handler;
    }

    public final String[][] GetFieldOfView()
    {
        return this.handler_ref.GetCurrentFoVData();
    }
}
