package edu.ozu.drone.utils;

import java.util.*;

public class AStar {

    public static List<String> calculateWithConstraints(Point start, Point dest, String[][] constraints_with_time)
    {
        // parse constraints
        HashMap<String, ArrayList<String>> occupied_list = new HashMap<>();
        for (String[] constraint : constraints_with_time)
        {
            ArrayList<String> vals = occupied_list.getOrDefault(constraint[0], new ArrayList<>());
            vals.add(constraint[1]);
            occupied_list.put(constraint[0], vals);
        }

        return new AStar().run(start, dest, occupied_list);
    }

    public static List<String> calculate(Point start, Point dest)
    {
        return new AStar().run(start, dest, new HashMap<>());
    }

    //<editor-fold defaultstate="collapsed" desc="A-Star implementation">
    private List<String> run(Point start, Point goal, HashMap<String, ArrayList<String>> occupiedList) {
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

            List<Point> neighbours = getNeighbours(current, T, occupiedList);
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
    private List<Point> getNeighbours(Point point, int t, HashMap<String, ArrayList<String>> occupiedList) {
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
            if (occupiedList.containsKey(n.key))
            {
                if (occupiedList.get(n.key).contains(String.valueOf(t)))
                {
                    continue;
                }
            }
            nodes.add(n);
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

    private static class AStarNode implements Comparable<AStarNode> {
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
}
