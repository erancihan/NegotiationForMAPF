package edu.ozu.mapp.agent;

import edu.ozu.mapp.agent.client.AgentHandler;
import edu.ozu.mapp.agent.client.helpers.FileLogger;
import edu.ozu.mapp.agent.client.models.Contract;
import edu.ozu.mapp.dataTypes.Constraint;
import edu.ozu.mapp.keys.AgentKeys;
import edu.ozu.mapp.keys.KeyHandler;
import edu.ozu.mapp.system.Broadcast;
import edu.ozu.mapp.system.FoV;
import edu.ozu.mapp.system.WorldOverseer;
import edu.ozu.mapp.utils.*;
import edu.ozu.mapp.utils.bid.BidSpace;
import edu.ozu.mapp.utils.path.Path;
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

    public WorldOverseer worldOverseerReference;

    /*
     * -1 | just started, nothing present
     *  0 | lost
     *  1 | won
     * */
    public int negotiation_result = -1;
    public String dimensions = "";

    public ArrayList<Constraint> constraints;

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

    public Agent(String agentName, String agentID, Point start, Point dest) {
        this(agentName, agentID, start, dest, Globals.INITIAL_TOKEN_BALANCE);
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

    public ArrayList<Constraint> prepareConstraints(ArrayList<Constraint> systemConstraintSet)
    {
        ArrayList<Constraint> constraints = new ArrayList<>(systemConstraintSet);

        //todo resolve
//        constraints.addAll(GetFoVasConstraint());

        return constraints;
    }

    private ArrayList<Constraint> genConstraints(ArrayList<Constraint> constraints)
    {
        HashSet<Constraint> constraintsSet = new HashSet<>();
        constraintsSet.addAll(constraints);
        constraintsSet.addAll(this.constraints);

        constraintsSet.addAll(prepareConstraints(new ArrayList<>(constraintsSet)));

        return new ArrayList<>(constraintsSet);
    }

    private ArrayList<Constraint> genConstraints()
    {
        return genConstraints(new ArrayList<>());
    }

    private HashMap<String, ArrayList<String>> constraints2hashmap(ArrayList<Constraint> constraints)
    {
        HashMap<String, ArrayList<String>> _map = new HashMap<>();

        for (Constraint constraint : constraints)
        {   // fill hash map
            if (!_map.containsKey(constraint.location.key))
            {   // init constraint if hash map doesn't have it
                _map.put(constraint.location.key, new ArrayList<>());
            }

            String __t = constraint.at_t == -1 ? "inf" : String.valueOf(constraint.at_t);
            if (!_map.get(constraint.location.key).contains(__t))
            {   // append time detail of constraint
                _map.get(constraint.location.key).add(__t);
            }
        }

        return  _map;
    }

    public final List<String> calculatePath(Point start, Point dest, ArrayList<Constraint> constraints)
    {
        HashMap<String, ArrayList<String>> _constraints = constraints2hashmap(genConstraints(constraints));

        logger.debug(AGENT_ID + " | calculating A* {" + start + ", " + dest + ", " + _constraints + ", " + dimensions + ", " + time + "}");
        return new AStar().calculate(start, dest, _constraints, dimensions, time);
    }

    public final List<String> calculatePath(Point start, Point dest, String Ox)
    {   // convert Ox to constraints
        ArrayList<Constraint> constraints = new ArrayList<>();
        Path _ox = new Path(Ox);
        for (int i = 0; i < _ox.size(); i++) {
            constraints.add(new Constraint(_ox.get(i), time + i));
        }

        return calculatePath(start, dest, constraints);
    }

    public final List<String> calculatePath(Point start, Point dest)
    {
        return calculatePath(start, dest, new ArrayList<>());
    }

    public List<String> calculatePath()
    {
        return calculatePath(START, DEST);
    }

    public double UtilityFunction(SearchInfo search)
    {
        // how far is the last point to destination
        double offset = 0;
        if (DEST != null) {
            offset = search.Path.getLast().ManhattanDistTo(DEST) * 1E-5;
        }

        return (1 - ((search.PathSize - search.MinPathSize) / (search.MaxPathSize - search.MinPathSize)) - offset);
    }


    public final List<Bid> GetBidSpace(Point From, Point To, int deadline)
    {
        BidSpace space = new BidSpace();
        space.init(
                From,
                To,
                deadline,
                constraints2hashmap(genConstraints()),
                this.dimensions.isEmpty() ? "0x0" : this.dimensions,
                time
        );
        space.prepare();

        double max = Double.MIN_VALUE;
        double min = Double.MAX_VALUE;
        List<Path> paths = new ArrayList<>();
        for (int i = 0; i < Globals.MAX_BID_SPACE_POOL_SIZE; i++)
        {
            Path next = space.next();
            if (next == null) break;
            if (next.size() == 0) break;

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
                    UtilityFunction(new SearchInfo(max, min, path))
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

        if (to != null && !to.equals(POS))
        {
            return GetBidSpace(POS, to, Globals.FIELD_OF_VIEW_SIZE);
        }

        logger.debug("selecting DEST for exit point");
        return GetBidSpace(POS, DEST, Globals.FIELD_OF_VIEW_SIZE);
    }

    public final ArrayList<Constraint> GetFoVasConstraint()
    {
        ArrayList<Constraint> constraints = new ArrayList<>();

        FoV fov = worldOverseerReference.GetFoV(this.AGENT_ID);
        for (Broadcast broadcast : fov.broadcasts) {
            if (broadcast.agent_name.equals(this.AGENT_ID)) {
                // skip broadcast if it is own
                System.out.println("> skip own");
                continue;
            }
            constraints.addAll(broadcast.locations);
        }

        return constraints;
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
        return new Path(worldOverseerReference.GetBroadcast(current_opponent));
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
        return worldOverseerReference.GetMyContract(this);
    }

    private AgentHandler handler_ref;
    public final void SetHandlerRef(AgentHandler handler)
    {
        handler_ref = handler;
    }

    public final FoV GetFieldOfView()
    {
//        return this.handler_ref.GetCurrentFoVData();
        return this.worldOverseerReference.GetFoV(this.AGENT_NAME);
    }
}
