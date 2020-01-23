package edu.ozu.drone.agent;

import edu.ozu.drone.utils.*;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class Agent {
    public static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Agent.class);

    public String AGENT_NAME, AGENT_ID;
    public Point START, DEST;

    public boolean isHeadless = false;

    public Point POS;
    public List<String> path;
    public int time = 0;

    public abstract void init();
    public abstract Action onMakeAction();
    public abstract void onReceiveState(edu.ozu.drone.utils.State state);

    public void run()
    {
        logger.info("calculating path");
        path = calculatePath();

        POS = new Point(path.get(0).split("-"));
    }

    public List<String> calculatePath()
    {
        return calculatePath(START, DEST);
    }

    public List<String> calculatePath(Point start, Point dest)
    {
        return AStar.calculate(start, dest);
    }

    public List<String> getBidSpace()
    {
        // TODO CODE
        return path;
    }

    public void acceptLastBids(JSONNegotiationSession json)
    {
        String[] path = null;
        for (String[] bid : json.bids)
        {
            if (bid[0].equals("agent:" + AGENT_ID))
            {   // fetch own accepted path
                path = bid[1].replaceAll("([\\[\\]]*)", "").split(",");

                break;
            }
        }
        Assert.notNull(path, "Accepted PATH should not be null!");

        // acknowledge negotiation result and calculate from its last point to the goal
        String[] end = path[path.length-1].split("-");
        // recalculate path starting from the end point of agreed path
        List<String> rest = calculatePath(new Point(Integer.parseInt(end[0]), Integer.parseInt(end[1])), DEST);

        // ...glue them together
        List<String> new_path = new ArrayList<>(Arrays.asList(path));
        // ensure that connection points match
        Assert.isTrue(
                new_path.get(new_path.size() - 1).equals(rest.get(0)),
                "Something went wrong while accepting last bids!"
        );
        for (int idx = 1; idx < rest.size(); idx++)
        { // merge...
            new_path.add(rest.get(idx));
        }
        // commit to global
        this.path = new_path;
    }

    //<editor-fold defaultstate="collapsed" desc="Get Broadcast">
    public String getBroadcast()
    {
        return edu.ozu.drone.utils.Utils.toString(getBroadcastArray(this.time), ",");
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Get Next Broadcast">
    public Object getNextBroadcast()
    {
        return edu.ozu.drone.utils.Utils.toString(getBroadcastArray(this.time + 1), ",") ;
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Get Broadcast Array">
    public String[] getBroadcastArray()
    {
        return getBroadcastArray(time);
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Get Broadcast Array (int time) ">
    public String[] getBroadcastArray(int time)
    {
        List<String> broadcast = new ArrayList<>();
        for (int i = 0; (i < Globals.BROADCAST_SIZE) && (i + time < path.size()); i++)
        {
            broadcast.add(path.get(i + time));
        }

        return broadcast.toArray(new String[0]);
    }
    //</editor-fold>

    public void move(JSONAgent response)
    {
        time = time + 1;
        Point nextPoint =  new Point(path.get(Math.min(time, path.size() - 1)).split("-"));
        Assert.isTrue(
                (response.agent_x+"-"+response.agent_y).equals(nextPoint.key),
                "next point and move action does not match! \n" +
                response.agent_x + "-" + response.agent_y + " != " +
                nextPoint.key
        );

        POS = nextPoint;
    }
}
