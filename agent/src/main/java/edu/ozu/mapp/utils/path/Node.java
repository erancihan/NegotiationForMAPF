package edu.ozu.mapp.utils.path;

import edu.ozu.mapp.utils.Point;

import java.util.*;
import java.util.stream.Collectors;

public class Node implements Comparable<Node>, Cloneable {
    public Point point;
    public double dist;

    public int time;
    public LinkedList<Node> path;

    public Node(Point point) {
        this.point = point;
        this.path = new LinkedList<>();
    }

    public Node(Point point, int time) {
        this(point);
        this.time = time;
    }

    public Node(Point point, double dist) {
        this(point);
        this.dist = dist;
    }

    public Node(Point point, double dist, int time) {
        this(point, dist);
        this.time = time;
    }

    public List<Node> getNeighbours(HashMap<String, ArrayList<String>> constraints, int bound_r, int bound_b)
    {
        return getNeighbours(null, constraints, bound_r, bound_b);
    }

    public List<Node> getNeighbours(Point goal, HashMap<String, ArrayList<String>> constraints, int bound_r, int bound_b)
    {
        return getNeighbours(goal, constraints, 0, bound_r, 0, bound_b);
    }

    public List<Node> getNeighbours(Point goal, HashMap<String, ArrayList<String>> constraints, int bound_l, int bound_r, int bound_t, int bound_b)
    {
        List<Node> nodes = new ArrayList<>();
        for (int i = 0; i < 9; i++)
        {   // BEGIN:FOR | NEIGHBOURHOOD SEARCH
            /*
             * On a grid, iterates & calculates points.
             *  it can be mapped respectively as following
             *
             * Dx := (i % 3) - 1
             * Dy := (i / 3) - 1
             *
             * [ 0 , 1 , 2 ]          [ (-1,-1) , ( 0,-1) , ( 1,-1) ]
             * [ 3 , 4 , 5 ]    ->    [ (-1, 0) , ( 0, 0) , ( 1, 0) ]
             * [ 6 , 7 , 8 ]          [ (-1, 1) , ( 0, 1) , ( 1, 1) ]
             */
            // skip inter-cardinals & center
            if (i % 2 == 0) continue;

            int x = point.x + (i % 3) - 1;
            int y = point.y + (i / 3) - 1;

            if (x < bound_l || bound_r <= x) continue;  // x : [bound_l, bound_r) | i.e. [0, 16) -> x : 0, 1, ... 15
            if (y < bound_t || bound_b <= y) continue;  // y : [bound_t, bound_b) | i.e. [0, 16) -> y : 0, 1, ... 15

            Point neighbour = new Point(x, y);
            // Check for Vertex Constraint
            if (constraints.containsKey(neighbour.key))
            {
                // There is an entry in constraints for this location
                // When navigated to next neighbour, neighbour will be in next time step
                if (
                    constraints.get(neighbour.key).contains(String.valueOf(time + 1)) ||
                    constraints.get(neighbour.key).contains("inf")  // if the location is infinitely occupied
                ) {
                    continue;
                }
            }

            // Check for Swap Constraint
            // todo

            // When navigated to next neighbour, they will
            //  be in the next time step from this node.
            nodes.add(
                new Node(
                    neighbour,
                    goal == null ? 0 : neighbour.ManhattanDistTo(goal),
                    (time + 1)
                )
            );

        }   // END:FOR

        // Add self for cyclic dep
//        nodes.add(new Node(point, time + 1));

        return nodes;
    }

    @Override
    public Node clone() throws CloneNotSupportedException {
        return (Node) super.clone();
    }

    @Override
    public boolean equals(Object that) {
        if (this == that) return true;
        if (!(that instanceof Node)) return false;

        Node node = (Node) that;
        return time == node.time && point.equals(node.point);
    }

    @Override
    public int hashCode() {
        return Objects.hash(point, dist, time);
    }

    @Override
    public int compareTo(Node o) {
        return Double.compare(this.dist, o.dist);
    }

    @Override
    public String toString() {
        return String.format("{%s, %d, %.2f}", point.key, time, dist);
    }

    public LinkedList<Node> linkTo(Node current) {
        path = new LinkedList<>(current.path);
        path.add(current);

        return path;
    }

    public Point[] Path2Array()
    {
        return path.stream().map(node -> node.point).toArray(Point[]::new);
    }

    public List<Point> Path2List()
    {
        return path.stream().map(node -> node.point).collect(Collectors.toList());
    }
}
