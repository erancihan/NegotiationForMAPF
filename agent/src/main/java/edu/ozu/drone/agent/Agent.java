package edu.ozu.drone.agent;

import edu.ozu.drone.utils.*;
import org.springframework.util.Assert;

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

        // todo acknowledge next path

        // todo recalculate
    }

    //<editor-fold defaultstate="collapsed" desc="Get Broadcast">
    public String getBroadcast()
    {
        String[] b = getBroadcastArray();

        return edu.ozu.drone.utils.Utils.toString(b, ",");
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Get Broadcast Array">
    public String[] getBroadcastArray()
    {
        String[] broadcast = new String[Globals.BROADCAST_SIZE];
        for (int i = time; (i < path.size()) && (i < time + broadcast.length); i++)
        {
            broadcast[i] = path.get(i);
        }

        return broadcast;
    }
    //</editor-fold>
}
