package edu.ozu.mapp.utils.path;

import edu.ozu.mapp.utils.Point;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
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
