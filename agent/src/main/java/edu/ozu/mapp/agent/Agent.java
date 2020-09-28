package edu.ozu.mapp.agent;

import edu.ozu.mapp.agent.client.helpers.FileLogger;
import edu.ozu.mapp.agent.client.helpers.WorldHandler;
import edu.ozu.mapp.keys.AgentKeys;
import edu.ozu.mapp.keys.KeyHandler;
import edu.ozu.mapp.utils.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.PublicKey;
import java.util.*;
import java.util.function.Function;

public abstract class Agent {
    public static final Logger logger = LoggerFactory.getLogger(Agent.class);
    private FileLogger fl;

    public String AGENT_NAME, AGENT_ID;
    public Point START, DEST;
    public HashMap<String, HashSet<String>> history = new HashMap<>();

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
        this.AGENT_NAME = agentName;
        this.AGENT_ID   = agentID;
        this.START      = start;
        this.DEST       = dest;

        this.isHeadless = true; // unless client says so
        fl = new FileLogger().CreateAgentLogger(AGENT_ID);

        history = new HashMap<String, HashSet<String>>();
        // create and store agent keys
        keys = KeyHandler.create(this);
    }

    public void init() { }

    public void PreNegotiation() { }

    public abstract Action onMakeAction();

    public void PostNegotiation() { }

    public void onReceiveState(State state) { }

    public void run()
    {
        logger.info("calculating path");
        path = calculatePath();

        POS = new Point(path.get(0).split("-"));
        history = new HashMap<>();
    }

    public List<String> calculatePath() {
        return calculatePath(START, DEST);
    }

    public List<String> calculatePath(Point start, Point dest)
    {
        return AStar.calculate(start, dest, dimensions);
    }

    public List<Bid> GetBidSpace(Point From, Point To, int deadline)
    {
        BFS search;
        if (this.dimensions.isEmpty()) {
            search = new BFS(From, To, Globals.FIELD_OF_VIEW_SIZE).init();
        } else {
            String[] ds = dimensions.split("x");
            int width = Integer.parseInt(ds[0]);
            int height = Integer.parseInt(ds[1]);

            search = new BFS(From, To, Globals.FIELD_OF_VIEW_SIZE / 2, deadline, width, height);
            search.SetMinimumPathLength(Globals.FIELD_OF_VIEW_SIZE / 2);
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

    public List<Bid> GetBidSpace(Point From, Point To)
    {
        return GetBidSpace(From, To, Globals.FIELD_OF_VIEW_SIZE);
    }

    public List<Bid> GetCurrentBidSpace(int minimum_path_size)
    {
        return GetBidSpace(POS, DEST, minimum_path_size);
    }

    public List<Bid> GetCurrentBidSpace()
    {
        return GetBidSpace(POS, DEST, Globals.FIELD_OF_VIEW_SIZE);
    }


    public void OnAcceptLastBids(JSONNegotiationSession json) { }

    //<editor-fold defaultstate="collapsed" desc="Get Broadcast">
    public String getBroadcast() {
        return Utils.toString(GetOwnBroadcastPath(this.time), ",");
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Get Next Broadcast">
    public String getNextBroadcast() {
        return Utils.toString(GetOwnBroadcastPath(this.time + 1), ",");
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Get Broadcast Array">
    public String[] GetOwnBroadcastPath() {
        return GetOwnBroadcastPath(time);
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Get Broadcast Array (int time) ">
    public String[] GetOwnBroadcastPath(int time) {
        List<String> broadcast = new ArrayList<>();
        for (int i = 0; (i < Globals.BROADCAST_SIZE) && (i + time < path.size()); i++) {
            broadcast.add(path.get(i + time));
        }

        return broadcast.toArray(new String[0]);
    }
    //</editor-fold>

    public void OnMove(JSONAgent response) { }

    public void setWORLD_ID(String WORLD_ID) {
        fl.setWorldID(WORLD_ID);
        this.WORLD_ID = WORLD_ID;
    }

    private int GetTokenBalance()
    {
        if (WORLD_ID.isEmpty()) {
            logger.error("world id is empty!");
            return Globals.INITIAL_TOKEN_BALANCE;
        }

        return WorldHandler.getTokenBalance(WORLD_ID, AGENT_ID);
    }

    public HashSet<String> getOwnBidHistory() {
        return history.containsKey(AGENT_ID) ? history.get(AGENT_ID) : new HashSet<String>();
    }

    public String Encrypt(String text)
    {
        return KeyHandler.encrypt(text, keys.GetPublicKey());
    }

    public String Decrypt(String text)
    {
        return KeyHandler.decrypt(text, keys.GetPrivateKey(this));
    }

    public PublicKey GetPubKey()
    {
        return keys.GetPublicKey();
    }

    public void logNegoPre(String session_id) {
        fl.logAgentPreNego(session_id, this);
    }

    public void LogNegotiationOver(String prev_bidding_agent, String session_id)
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

    public void SetConflictLocation(String conflictLocation) {
        this.conflictLocation = conflictLocation;
    }

    public String GetConflictLocation() {
        return conflictLocation;
    }

    public void OnContractUpdated(String O)
    {
        if (!history.containsKey(AGENT_ID))
        {
            history.put(AGENT_ID, new HashSet<String>());
        }

        history.get(AGENT_ID).add(O);
    }

    public String GetCurrentTokenC() {
        return String.valueOf(WorldHandler.getTokenBalance(WORLD_ID, AGENT_ID));
    }

    public void LogNegotiationState(String prev_bidding_agent)
    {
        fl.LogAgentNegotiationState(prev_bidding_agent, this);
    }

    /**
     * This function is invoked after an action is made.
     * If action type is accept, that indicates Agent accepted
     * opponents bid & has LOST the negotiation.
     * */
    public void LogNegotiationState(String prev_bidding_agent, Action action)
    {
        if (action.type == ActionType.ACCEPT) {
            negotiation_result = 0;
            fl.LogAgentNegotiationState(prev_bidding_agent, this, true);
        } else {
            fl.LogAgentNegotiationState(prev_bidding_agent, this, false);
        }
    }
}
