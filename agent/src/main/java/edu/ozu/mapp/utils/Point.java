package edu.ozu.mapp.utils;

import org.springframework.util.Assert;

public class Point implements Comparable<Point> {
    public int x, y;
    public String key;

    public Point(int x, int y) {
        this.x = x;
        this.y = y;

        this.key = x + "-" + y;
    }

    public Point(String agent_position_datum, String delim)
    {
        this(agent_position_datum.split(delim));
    }

    public Point(String[] split)
    {
        Assert.isTrue(split.length == 2, "Split size for Point construct should be 2");

        x = Integer.parseInt(split[0]);
        y = Integer.parseInt(split[1]);

        key = x + "-" + y;
    }

    public double ManhattanDistTo(Point to) {
        return Math.abs(x - to.x) + Math.abs(y - to.y);
    }

    @Override
    public boolean equals(Object that) {
        if (that instanceof Point)
            return (x == ((Point) that).x) && (y == ((Point) that).y);
        else
            return false;
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }

    @Override
    public int compareTo(Point that) {
        return this.key.compareTo(that.key);
    }

    @Override
    public String toString() {
        return "Point{" + key + '}';
    }
}
