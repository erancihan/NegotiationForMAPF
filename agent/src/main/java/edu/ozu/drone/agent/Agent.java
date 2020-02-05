package edu.ozu.drone.agent;

import edu.ozu.drone.client.handlers.World;
import edu.ozu.drone.utils.*;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public abstract class Agent {
    public static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Agent.class);

    public String AGENT_NAME, AGENT_ID;
    public Point START, DEST;
    public HashMap<String, ArrayList<BidStruct>> history;

    public boolean isHeadless = false;

    public Point POS;
    public List<String> path;
    public int time = 0;

    public String WORLD_ID;

    public Agent(String agentName, String agentID, Point start, Point dest)
    {
        this.AGENT_NAME = agentName;
        this.AGENT_ID   = agentID;
        this.START      = start;
        this.DEST       = dest;

        this.isHeadless = true; // unless client says so
    }

    public void init() { }

    public abstract void preNegotiation();

    public abstract Action onMakeAction();

    public void postNegotiation() { }

    public void onReceiveState(State state) {
        // update current state info
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

    public List<String> getBidSpace() {
        // TODO CODE
        return path;
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
        return edu.ozu.drone.utils.Utils.toString(getBroadcastArray(this.time), ",");
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Get Next Broadcast">
    public String getNextBroadcast() {
        return edu.ozu.drone.utils.Utils.toString(getBroadcastArray(this.time + 1), ",");
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

    public int getTokenBalance() {
        if (WORLD_ID.isEmpty()) {
            logger.error("world id is empty!");
            return Globals.INITIAL_TOKEN_BALANCE;
        }

        return World.getTokenBalance(WORLD_ID, AGENT_ID);
    }
}
