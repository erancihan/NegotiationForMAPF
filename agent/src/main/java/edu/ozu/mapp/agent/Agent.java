package edu.ozu.mapp.agent;

import edu.ozu.mapp.agent.client.NegotiationSession;
import edu.ozu.mapp.agent.client.handlers.World;
import edu.ozu.mapp.agent.client.models.Contract;
import edu.ozu.mapp.keys.AgentKeys;
import edu.ozu.mapp.keys.KeyHandler;
import edu.ozu.mapp.utils.*;
import org.springframework.util.Assert;

import java.security.PublicKey;
import java.util.*;

public abstract class Agent {
    public static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Agent.class);

    public String AGENT_NAME, AGENT_ID;
    public Point START, DEST;
    public HashMap<String, HashSet<String>> history = new HashMap<>();

    public boolean isHeadless = false;

    public Point POS;
    public List<String> path;
    public int time = 0;

    public String WORLD_ID;
    public AgentKeys keys;

    public Agent(String agentName, String agentID, Point start, Point dest)
    {
        this.AGENT_NAME = agentName;
        this.AGENT_ID   = agentID;
        this.START      = start;
        this.DEST       = dest;

        this.isHeadless = true; // unless client says so

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

    public List<String> calculatePath(Point start, Point dest) {
        return AStar.calculate(start, dest);
    }

    public List<Bid> GetBidSpace(Point From, Point To)
    {
        BFS search = new BFS(From, To, Globals.FIELD_OF_VIEW_SIZE).init();

        PriorityQueue<Bid> bids = new PriorityQueue<>();
        for (Path path : search.paths)
        {
            if (path.contains(To)) bids.add(
                new Bid(AGENT_ID, path, (Integer x) -> (double) (1 - ((x - search.Min) / (search.Max - search.Min))))
            );
        }

        return new ArrayList<>(bids);
    }

    public List<Bid> GetCurrentBidSpace()
    {
        return GetBidSpace(POS, DEST);
    }

    //<editor-fold defaultstate="collapsed" desc="Accept Last Bids">
    public void acceptLastBids(JSONNegotiationSession json) {
        String[] accepted_path = null;
        for (String[] bid : json.bids) {
            if (bid[0].equals("agent:" + AGENT_ID)) {   // fetch own accepted path
                accepted_path = bid[1].split(":")[0].replaceAll("([\\[\\]]*)", "").split(",");

                break;
            }
        }
        Assert.notNull(accepted_path, "Accepted PATH should not be null!");
        Assert.isTrue(accepted_path.length > 0, "Accepted PATH should not be empty!");

        // acknowledge negotiation result and calculate from its last point to the goal
        String[] end = accepted_path[accepted_path.length - 1].split("-");
        // recalculate path starting from the end point of agreed path
        List<String> rest = calculatePath(new Point(Integer.parseInt(end[0]), Integer.parseInt(end[1])), DEST);

        // ...glue them together
        List<String> new_path = new ArrayList<>();
        for (int idx = 0; idx < path.size() && !path.get(idx).equals(POS.key); idx++) { // prepend path so far until current POS
            new_path.add(path.get(idx));
        }
        new_path.add(POS.key); // add current POS
        for (int idx = 0; idx < accepted_path.length; idx++) { // add accepted paths
            if (idx == 0 && accepted_path[idx].equals(POS.key)) {
                continue; // skip if first index is current POS, as it is already added
            }
            new_path.add(accepted_path[idx]);
        }
        // ensure that connection points match
        Assert.isTrue(
                new_path.get(new_path.size() - 1).equals(rest.get(0)),
                "Something went wrong while accepting last bids!"
        );
        for (int idx = 1; idx < rest.size(); idx++) { // merge...
            new_path.add(rest.get(idx));
        }
        // commit to global
        this.path = new_path;

        World.doBroadcast(WORLD_ID, AGENT_ID, getBroadcastArray());
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Get Broadcast">
    public String getBroadcast() {
        return edu.ozu.mapp.utils.Utils.toString(getBroadcastArray(this.time), ",");
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Get Next Broadcast">
    public String getNextBroadcast() {
        return edu.ozu.mapp.utils.Utils.toString(getBroadcastArray(this.time + 1), ",");
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
        time = time + 1;
        Point nextPoint = new Point(path.get(Math.min(time, path.size() - 1)).split("-"));
        Assert.isTrue(
                (response.agent_x + "-" + response.agent_y).equals(nextPoint.key),
                "next point and move action does not match! \n" +
                        response.agent_x + "-" + response.agent_y + " != " + nextPoint.key +
                        "\n PATH:" + path + "\n"
        );

        POS = nextPoint;
    }

    public void setWORLD_ID(String WORLD_ID) {
        this.WORLD_ID = WORLD_ID;
    }

    public int getTokenBalance()
    {
        if (WORLD_ID.isEmpty()) {
            logger.error("world id is empty!");
            return Globals.INITIAL_TOKEN_BALANCE;
        }

        return World.getTokenBalance(WORLD_ID, AGENT_ID);
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
     * */
    public Contract PrepareContract(NegotiationSession session)
    {
       return Contract.Create(this, session);
    }
}
