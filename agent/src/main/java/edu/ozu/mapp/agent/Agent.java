package edu.ozu.mapp.agent;

import edu.ozu.mapp.agent.client.NegotiationSession;
import edu.ozu.mapp.agent.client.helpers.FileLogger;
import edu.ozu.mapp.agent.client.helpers.Negotiation;
import edu.ozu.mapp.agent.client.helpers.WorldHandler;
import edu.ozu.mapp.agent.client.models.Contract;
import edu.ozu.mapp.keys.AgentKeys;
import edu.ozu.mapp.keys.KeyHandler;
import edu.ozu.mapp.utils.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import java.security.PublicKey;
import java.util.*;

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
    private String dimensions = "";

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

    public void preNegotiation() { }

    public abstract Action onMakeAction();

    public void postNegotiation() { }

    public void onReceiveState(State state)
    {
        // update current state info
        // TODO WTF!?
        /*
        for (String[] bid : state.bids) {   // [agentID, path:tokens]
            ArrayList<BidStruct> hist = history.getOrDefault(bid[0], new ArrayList<>());

            String[] b = bid[1].split(":");
            BidStruct bidStruct = new BidStruct(bid[0], b[0], Integer.parseInt(b[1]));

            if (hist.size() > 0) { // if there are elements
                if (!hist.get(hist.size()-1).equals(bidStruct)) { // and the last one is different
                    hist.add(bidStruct);
                }
            } else { // if there are no elements
                hist.add(bidStruct);
            }

            history.put(bid[0], hist);
        }
         */
    }

    public void run() {
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

    public List<Bid> GetBidSpace(Point From, Point To)
    {
        BFS search;
        if (this.dimensions.isEmpty()) {
            search = new BFS(From, To, Globals.FIELD_OF_VIEW_SIZE).init();
        } else {
            String[] ds = dimensions.split("x");
            int width = Integer.parseInt(ds[0]);
            int height = Integer.parseInt(ds[1]);

            search = new BFS(From, To, Globals.FIELD_OF_VIEW_SIZE, width, height).init();
        }

        PriorityQueue<Bid> bids = new PriorityQueue<>();
        for (Path path : search.paths)
        {
            if (path.contains(To)) bids.add(
                new Bid(AGENT_ID, path, (Double x) -> 1 - ((x - search.Min) / (search.Max - search.Min)))
            );
        }

        return new ArrayList<>(bids);
    }

    public List<Bid> GetCurrentBidSpace()
    {
        return GetBidSpace(POS, DEST);
    }

    @SuppressWarnings("Duplicates")
    //<editor-fold defaultstate="collapsed" desc="Accept Last Bids">
    public void  acceptLastBids(JSONNegotiationSession json) {
        // get contract
        Contract contract = Negotiation.getContract(this);
        logger.debug("AcceptLastBids:"+contract);
        logger.debug("          json:"+json);

        List<String> new_path = new ArrayList<>();

        // use contract to apply select paths
        // if 'x' is self, update planned path
        if (contract.x.equals(this.AGENT_ID)) {
            // WIN condition
            logger.debug("x is self | {contract.x:"+contract.x + " == a_id:" + this.AGENT_ID + "}");

            String[] Ox = contract.Ox.replaceAll("([\\[\\]]*)", "").split(",");

            logger.debug("{current POS:" + this.POS + " == Ox[0]:" + Ox[0] + "}");
            Assert.isTrue(this.POS.equals(new Point(Ox[0].split("-"))), "");

            // acknowledge negotiation result and calculate from its last point to the goal
            Point end = new Point(Ox[Ox.length - 1].split("-"));
            // recalculate path starting from the end point of agreed path
            logger.debug("{accepted_path:" + Arrays.toString(Ox) + "}");
            List<String> rest = calculatePath(end, DEST);

            // ...glue them together
            new_path = new ArrayList<>();
            for (int idx = 0; idx < path.size() && !path.get(idx).equals(POS.key); idx++)
            {   // prepend path so far until current POS
                // for history purposes
                new_path.add(path.get(idx));
            }
            new_path.add(POS.key); // add current POS
            for (int idx = 0; idx < Ox.length; idx++)
            {   // add accepted paths
                if (idx == 0 && Ox[idx].equals(POS.key))
                {   // skip if first index is current POS, as it is already added
                    continue;
                }
                new_path.add(Ox[idx]);
            }

            // ensure that connection points match
            Assert.isTrue(
                    new_path.get(new_path.size() - 1).equals(rest.get(0)),
                    "Something went wrong while accepting last bids!"
            );

            // merge...
            for (int idx = 1; idx < rest.size(); idx++)
            {
                new_path.add(rest.get(idx));
            }
            winC++;
        } else {
            // else use 'Ox' & others as constraint & re-calculate path
            // LOSE condition
            logger.debug("x is not self | {contract.x:"+contract.x + " != a_id:" + this.AGENT_ID + "}");

            String[] Ox = contract.Ox.replaceAll("([\\[\\]]*)", "").split(",");

            // create constraints
            ArrayList<String[]> constraints = new ArrayList<>();
            for (int i = 0; i < Ox.length; i++)
            {   // Add Ox as constraint
                constraints.add(new String[]{Ox[i], String.valueOf(this.time + i)});
            }
            // TODO add from FoV

            List<String> rest = AStar.calculateWithConstraints(POS, DEST, constraints.toArray(new String[0][0]));

            new_path = new ArrayList<>();
            for (int idx = 0; idx < path.size() && !path.get(idx).equals(POS.key); idx++)
            {   // prepend path so far until current POS
                // for history purposes
                new_path.add(path.get(idx));
            }

            // ensure that connection points match
            Assert.isTrue(POS.key.equals(rest.get(0)), "Something went wrong while accepting last bids!");
            Assert.isTrue(DEST.key.equals(rest.get(rest.size() - 1)), "Something went wrong while accepting last bids!");

            // merge...
            // since current POS is already in 'rest'@0, we can just add it
            new_path.addAll(rest);
            loseC++;
        }

        // update global path
        logger.debug(this.AGENT_ID + "{path:" + this.path + "}");
        this.path = new_path;
        logger.debug(this.AGENT_ID + "{path:" + this.path + "}");

        WorldHandler.doBroadcast(WORLD_ID, AGENT_ID, getBroadcastArray());
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Get Broadcast">
    public String getBroadcast() {
        return Utils.toString(getBroadcastArray(this.time), ",");
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Get Next Broadcast">
    public String getNextBroadcast() {
        return Utils.toString(getBroadcastArray(this.time + 1), ",");
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Get Broadcast Array">
    public String[] getBroadcastArray() {
        return getBroadcastArray(time);
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Get Broadcast Array (int time) ">
    public String[] getBroadcastArray(int time) {
        List<String> broadcast = new ArrayList<>();
        for (int i = 0; (i < Globals.BROADCAST_SIZE) && (i + time < path.size()); i++) {
            broadcast.add(path.get(i + time));
        }

        return broadcast.toArray(new String[0]);
    }
    //</editor-fold>

    public void move(JSONAgent response) {
        // update internal clock
        time = time + 1;

        // get next point
        Point nextPoint = new Point(path.get(Math.min(time, path.size() - 1)).split("-"));
        // validate next location
        Assert.isTrue(
                (response.agent_x + "-" + response.agent_y).equals(nextPoint.key),
                "next point and move action does not match! \n" +
                        response.agent_x + "-" + response.agent_y + " != " + nextPoint.key +
                        "\n PATH:" + path + "\n"
        );

        // update current position
        POS = nextPoint;
    }

    public void setWORLD_ID(String WORLD_ID) {
        fl.setWorldID(WORLD_ID);
        this.WORLD_ID = WORLD_ID;
    }

    public int getTokenBalance()
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

    /**
     * Invoked only when negotiation is initiated, and agent
     * is about to join. (Before {@link #preNegotiation()})
     *
     * Negotiation session status should be "join" for this
     * function to be invoked
     *
     * Fills the empty contract with necessary information.
     *
     * Mark that a new session has started
     * */
    public Contract PrepareContract(NegotiationSession session)
    {
        negotiation_result = -1;
        return Contract.Create(this, session);
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

    public void join(String world_id)
    {
        setWORLD_ID(world_id);
        dimensions = WorldHandler.GetDimensions(world_id);
    }
}
