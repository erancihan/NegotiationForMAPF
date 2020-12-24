package edu.ozu.mapp.utils;

import java.util.*;

public class AStar {
    public static void main(String[] args) {
        HashMap<String, String[]> occupied = new HashMap<>();

        ArrayList<String[]> paths = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            ArrayList<String[]> _o = new ArrayList<>();
            for (String key: occupied.keySet()) {
                _o.add(occupied.get(key));
            }

            List<String> a = calculateWithConstraints(new Point(5, 5), new Point(10, 10), _o.toArray(new String[0][0]), 0);

            paths.add(a.toArray(new String[0]));

            occupied.put("9-10", new String[]{"9-10", "inf"});
        }

        System.out.println(Arrays.deepToString(paths.toArray(new String[0][])));
    }

    public static List<String> calculateWithConstraints(Point start, Point dest, String[][] constraints_with_time, int t)
    {
        // parse constraints
        HashMap<String, ArrayList<String>> occupied_list = new HashMap<>();
        for (String[] constraint : constraints_with_time)
        {
            ArrayList<String> vals = occupied_list.getOrDefault(constraint[0], new ArrayList<>());
            vals.add(constraint[1]);
            occupied_list.put(constraint[0], vals);
        }

        return new AStar().run(start, dest, occupied_list, t);
    }

    public static List<String> calculate(Point start, Point dest)
    {
        return new AStar().run(start, dest, new HashMap<>(), "", 0);
    }

    public static List<String> calculate(Point start, Point dest, String Dimensions)
    {
        return new AStar().run(start, dest, new HashMap<>(), Dimensions, 0);
    }

    private List<String> run(Point start, Point goal, HashMap<String, ArrayList<String>> occupiedList, int t)
    {
        return run(start, goal, occupiedList, "", t);
    }

    //<editor-fold defaultstate="collapsed" desc="A-Star implementation">
    private List<String> run(Point start, Point goal, HashMap<String, ArrayList<String>> occupiedList, String Dimensions, int T)
    {
        if (start.equals(goal))
            return Collections.singletonList(start.key);

        double inf = Double.MAX_VALUE;

        String[] ds = Dimensions.split("x");
        int width  = (ds.length == 2 && !ds[0].isEmpty() && !ds[0].equals("0")) ? Integer.parseInt(ds[0]) : Integer.MAX_VALUE;
        int height = (ds.length == 2 && !ds[1].isEmpty() && !ds[1].equals("0")) ? Integer.parseInt(ds[1]) : Integer.MAX_VALUE;

        HashMap<String, String> links = new HashMap<>();

        PriorityQueue<Node> open = new PriorityQueue<>();
        List<Node> closed = new ArrayList<>();
        HashMap<String, Double> g = new HashMap<>();
        g.put(start.key, 0.0);

        open.add(new Node(start, start.ManhattanDistTo(goal), T));

        while (!open.isEmpty())
        {
            Node current = open.remove();

            if (closed.contains(current)) continue;

            closed.add(current);

            if (current.point.equals(goal))
            {
                return constructPath(links, start, goal);
            }

            List<Node> neighbours = getNeighbours(current.point, current.t + 1, occupiedList, width, height);
            for (Node neighbour : neighbours)
            {
                if (closed.contains(neighbour)) continue;

                // d <- g[current] + COST(current, neighbour, g)
                double d = g.getOrDefault(current.point.key, inf) + current.point.ManhattanDistTo(neighbour.point);
                if (d < g.getOrDefault(neighbour.point.key, inf))
                {
                    g.put(neighbour.point.key, d);

                    links.put(neighbour.point.key, current.point.key);
                    neighbour.dist = d + neighbour.point.ManhattanDistTo(goal);

                    try { open.add(neighbour.clone()); }
                    catch (CloneNotSupportedException e) { e.printStackTrace(); }
                }
            }
        }

        return null;
    }

    // todo retrieve env dims
    private List<Node> getNeighbours(Point point, int t, HashMap<String, ArrayList<String>> occupiedList, int width, int height) {
        List<Node> nodes = new ArrayList<>();

        for (int i = 0; i < 9; i++) { // position of point
            if (i % 2 == 0) continue;

            int x = point.x + (i % 3) - 1;
            int y = point.y + (i / 3) - 1;

            if (x < 0 || x >= width) continue;  // x out of bounds
            if (y < 0 || y >= height) continue; // y out of bounds

            Node node = new Node(new Point(x, y), t);
            if (occupiedList.containsKey(node.point.key))
            {
                if (
                    occupiedList.get(node.point.key).contains(String.valueOf(t)) ||
                    occupiedList.get(node.point.key).contains("inf")
                )
                    continue;
            }
            nodes.add(node);
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
    //</editor-fold>
}

class Node implements Comparable<Node>, Cloneable {
    Point point;
    double dist;

    int t;

    Node(Point point)
    {
        this.point = point;
    }

    Node(Point point, int t)
    {
        this.point = point;
        this.t     = t;
    }

    Node(Point point, double dist)
    {
        this(point);
        this.dist = dist;
    }

    Node(Point point, double dist, int t)
    {
        this(point, dist);
        this.t = t;
    }

    @Override
    protected Node clone() throws CloneNotSupportedException {
        return (Node) super.clone();
    }

    @Override
    public boolean equals(Object that) {
        if (this == that) return true;
        if (!(that instanceof Node)) return false;

        Node node = (Node) that;
        return Double.compare(node.dist, dist) == 0 && t == node.t && point.equals(node.point);
    }

    @Override
    public int hashCode() {
        return Objects.hash(point, dist, t);
    }

    @Override
    public int compareTo(Node o) {
        return Double.compare(this.dist, o.dist);
    }

    @Override
    public String toString() {
        return String.format("%s:%.2f", point.key, dist);
    }
}
