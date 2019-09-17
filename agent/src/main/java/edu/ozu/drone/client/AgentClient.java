package edu.ozu.drone.client;

import edu.ozu.drone.client.ui.AgentUI;
import edu.ozu.drone.utils.Point;

import javax.swing.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AgentClient extends Runner {
    private AgentHandler handler;
    private boolean hasUI;

    protected String AGENT_NAME = "";
    protected String AGENT_ID   = "";
    protected Point START;
    protected Point DEST;

    Point POS;
    List<String> path;
    int time = 0;

    public AgentClient(boolean ui) {
        hasUI = ui;
        init();
        handler = new AgentHandler(this);
    }

    public AgentClient()
    {
        this(true);
    }

    public void init() { }

    void setHeadless(boolean b)
    {
        hasUI = !b;
    }

    public List<String> calculatePath(Point START, Point DEST)
    {
        return AStar(START, DEST);
    }

    void run()
    {
        System.out.println("> " + this + " calculating path");
        path = calculatePath(START, DEST);

        if (hasUI)
        {
            System.out.println("> " + this + "display ui");
            __launchUI();
        }
    }

    String getBroadcast()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = time; (i < path.size()) && (i < time + 3); i++)
        {
            sb.append(path.get(i));
            if ((i+1 < path.size()) && (i+1 < time + 3)) sb.append(",");
        }
        sb.append("]");
        return String.valueOf(sb);
    }

    //<editor-fold defaultstate="collapsed" desc="A-Star implementation">
    private List<String> AStar(Point start, Point goal) {
        int T = 0;
        double inf = Double.MAX_VALUE;

        HashMap<String, String> links = new HashMap<>();

        PriorityQueue<AStarNode> open = new PriorityQueue<>();
        List<Point> closed = new ArrayList<>();
        HashMap<String, Double> g = new HashMap<>();
        g.put(start.key, 0.0);

        open.add(new AStarNode(start, start.ManhattanDistTo(goal)));

        while (!open.isEmpty()) {
            AStarNode currentNode = open.remove();
            Point current = currentNode.point;

            if (closed.contains(current)) {
                continue;
            }

            closed.add(current);
            T = T + 1;

            if (current.equals(goal)) {
                return constructPath(links, start, goal);
            }

            List<Point> neighbours = getNeighbours(current, T);
            for (Point neighbour : neighbours) {
                if (closed.contains(neighbour)) {
                    continue;
                }

                // d <- g[current] + COST(current, neighbour, g)
                double d = g.getOrDefault(current.key, inf) + current.ManhattanDistTo(neighbour);

                if (d < g.getOrDefault(neighbour.key, inf)) {
                    g.put(neighbour.key, d);

                    links.put(neighbour.key, current.key);
                    d = d + neighbour.ManhattanDistTo(goal);
                    open.add(new AStarNode(neighbour, d));
                }
            }
        }
        return null;
    }

    // todo retrieve env dims
    private List<Point> getNeighbours(Point point, int t) {
        List<Point> nodes = new ArrayList<>();

        for (int i = 0; i < 9; i++) { // position of point
            if (i % 2 == 0) {
                continue;
            }

            int x = point.x + (i % 3) - 1;
            int y = point.y + (i / 3) - 1;

            if (x < 0 /*|| x >= env.width*/) { // x out of bounds
                continue;
            }
            if (y < 0 /*|| y >= env.height*/) { // y out of bounds
                continue;
            }

            Point n = new Point(x, y);
            nodes.add(n);
//            if (!env.isOccupiedAt(n.key, t)) { nodes.add(n); }
        }

        return nodes;
    }

    private List<String> constructPath(HashMap<String, String> links, Point start, Point goal) {
        List<String> path = new ArrayList<>();

        if (links.isEmpty()) {
            return path;
        }

        String next = goal.key;
        while (!next.equals(start.key)) {
            path.add(next);
            next = links.get(next);
        }
        path.add(start.key);

        Collections.reverse(path);

        return path;
    }

    private class AStarNode implements Comparable<AStarNode> {
        Point point;
        private double dist;

        AStarNode(Point point, double dist) {
            this.point = point;
            this.dist = dist;
        }

        @Override
        public int compareTo(AStarNode o) {
            return Double.compare(this.dist, o.dist);
        }

        @Override
        public String toString() {
            return String.format("%s:%.2f", point.key, dist);
        }
    }
    //</editor-fold>

    @SuppressWarnings("Duplicates")
    private void __launchUI()
    {
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html
         */
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex) {
            Logger.getLogger(AgentUI.class.getName()).log(Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> new AgentUI(this.handler).setVisible(true));
    }
}
