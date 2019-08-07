package edu.ozu.drone;

import java.util.*;

public class AgentClient extends Runner implements Runnable {
    private String WS_URL = "ws://";

    protected String AGENT_NAME = "";
    protected String AGENT_ID   = "";
    protected Point START;
    protected Point DEST;

    public AgentClient() { }

    @Override
    public void run() {
        System.out.println(AGENT_NAME);
    }

    public void init() { }

    private void join() {
        // todo
        // get path [0, 2]

        // join
        // post localhost:3001/join payload:{world_id, agent_id, agent_x, agent_y}

        // @response: watch()
    }

    private void move() {
        // post localhost:3001/move payload:{world_id, agent_id, agent_x, agent_y, direction}
        // direction -> {N, W, E, S}

        // response should match with next path point in line
    }

    private void watch() {
        // ws://localhost:3001/world/{world_id}/{agent_id}

        // collision check @ every incoming message
        hasCollisions();

        // ping server
    }

    private boolean hasCollisions() {
        return false;
    }

    public void pathToDest() {
        // run A*
        List<String> path = AStar(START, DEST);
    }

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
}
