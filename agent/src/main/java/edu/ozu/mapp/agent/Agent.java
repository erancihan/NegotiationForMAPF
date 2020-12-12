package edu.ozu.mapp.agent;

import edu.ozu.mapp.agent.client.AgentHandler;
import edu.ozu.mapp.agent.client.helpers.FileLogger;
import edu.ozu.mapp.agent.client.models.Contract;
import edu.ozu.mapp.keys.AgentKeys;
import edu.ozu.mapp.keys.KeyHandler;
import edu.ozu.mapp.system.NegotiationOverseer;
import edu.ozu.mapp.utils.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.PublicKey;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class Agent {
    public static final Logger logger = LoggerFactory.getLogger(Agent.class);
    private FileLogger fl;

    public String AGENT_NAME, AGENT_ID;
    public Point START, DEST;
    public History history;
    public int initial_tokens = Globals.INITIAL_TOKEN_BALANCE;
    public int current_tokens;

    public boolean isHeadless = false;

    public Point POS;
    public List<String> path;
    public int time = 0;

    public String WORLD_ID;
    public AgentKeys keys;
    private String conflictLocation;

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

        this.isHeadless = true; // unless client says so
        fl = new FileLogger().CreateAgentLogger(AGENT_ID);

        history = new History(AGENT_ID);
        // create and store agent keys
        keys = KeyHandler.create(this);
    }

    public void init() { }

    public void PreNegotiation(State state) { }

    public void onReceiveState(State state) { }

    public abstract Action onMakeAction(String negotiation_session_id);

    public void OnAcceptLastBids(JSONNegotiationSession json) { }

    public void PostNegotiation() { }

    public void OnMove(JSONAgent response) { }

    public final void run()
    {
        logger.info("calculating path");
        path = calculatePath();

        POS = new Point(path.get(0).split("-"));
        history = new History(AGENT_ID);
    }

    public List<String> calculatePath() {
        return calculatePath(START, DEST);
    }

    public List<String> calculatePath(Point start, Point dest)
    {
        return AStar.calculate(start, dest, dimensions);
    }

    public final List<Bid> GetBidSpace(Point From, Point To, int deadline)
    {
        BFS search;
        if (this.dimensions.isEmpty()) {
            search = new BFS(From, To, Globals.FIELD_OF_VIEW_SIZE).init();
        } else {
            String[] ds = dimensions.split("x");
            int width = Integer.parseInt(ds[0]);
            int height = Integer.parseInt(ds[1]);

            search = new BFS(From, To, Globals.FIELD_OF_VIEW_SIZE / 2, deadline, width, height);
            search.SetMinimumPathLength(Globals.BROADCAST_SIZE);
            search.init();
        }

        PriorityQueue<Bid> bids = new PriorityQueue<>(Comparator.reverseOrder());
        for (Path path : search.paths)
        {
            bids.add(
                    new Bid(AGENT_ID, path, UtilityFunction.apply(new SearchInfo(search.Max, search.Min, path)))
            );
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
        Point to = null;
        for (int i = broadcast.length-1; i >= 0; i--) {
            // iterate in reverse
            Point point = new Point(broadcast[i], "-");
            if (point.x <= (POS.x + Globals.FIELD_OF_VIEW_SIZE / 2) && point.y <= (POS.y + Globals.FIELD_OF_VIEW_SIZE / 2)) {
                to = point;
                break;
            }
        }

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

    public final void setWORLD_ID(String WORLD_ID)
    {
        fl.setWorldID(WORLD_ID);
        this.WORLD_ID = WORLD_ID;
    }

    private int GetTokenBalance()
    {
        if (WORLD_ID.isEmpty()) {
            logger.error("world id is empty!");
            return Globals.INITIAL_TOKEN_BALANCE;
        }

        return current_tokens;
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

    public final void logNegoPre(String session_id) {
        fl.logAgentPreNego(session_id, this);
    }

    public final void LogNegotiationOver(String prev_bidding_agent, String session_id)
    {
        /*
         * by the time this function is invoked,
         * if the negotiation result has not been updated,
         * it means that negotiation concluded without THIS agent accepting
         * SINCE NO TIMEOUT IS IMPLEMENTED YET
         * it indicates that opponent has accepted the bid & negotiation is won
         * */
        if (negotiation_result == -1)
        {
            negotiation_result = 1;
            fl.LogAgentNegotiationState(prev_bidding_agent, this, true);
        }
        fl.logAgentPostNego(session_id, this);
    }

    public final void SetConflictLocation(String conflictLocation)
    {
        this.conflictLocation = conflictLocation;
    }

    public final String GetConflictLocation()
    {
        return conflictLocation;
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

    public final void LogNegotiationState(String prev_bidding_agent)
    {
        fl.LogAgentNegotiationState(prev_bidding_agent, this);
    }

    /**
     * This function is invoked after an action is made.
     * If action type is accept, that indicates Agent accepted
     * opponents bid & has LOST the negotiation.
     * */
    public final void LogNegotiationState(String prev_bidding_agent, Action action)
    {
        if (action.type == ActionType.ACCEPT) {
            negotiation_result = 0;
            fl.LogAgentNegotiationState(prev_bidding_agent, this, true);
        } else {
            fl.LogAgentNegotiationState(prev_bidding_agent, this, false);
        }
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
