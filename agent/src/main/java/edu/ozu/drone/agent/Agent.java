package edu.ozu.drone.agent;

import edu.ozu.drone.utils.AStar;
import edu.ozu.drone.utils.Action;
import edu.ozu.drone.utils.Point;

import java.util.List;

public abstract class Agent {
    public String AGENT_NAME, AGENT_ID;
    public Point START, DEST;

    public boolean isHeadless = false;

    public Point POS;
    public List<String> path;
    public int time = 0;

    public abstract void init();
    public abstract Action onMakeAction();
    public abstract void onReceiveAction();

    public void run()
    {
        System.out.println("> " + this + " calculating path");
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

    //<editor-fold defaultstate="collapsed" desc="Get Broadcast">
    public String getBroadcast()
    {
        int broadcast_size = 3;

        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = time; (i < path.size()) && (i < time + broadcast_size); i++)
        {
            sb.append(path.get(i));
            if ((i+1 < path.size()) && (i+1 < time + broadcast_size)) sb.append(",");
        }
        sb.append("]");
        return String.valueOf(sb);
    }
    //</editor-fold>
}
