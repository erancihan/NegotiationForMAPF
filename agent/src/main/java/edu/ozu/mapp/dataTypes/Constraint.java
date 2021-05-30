package edu.ozu.mapp.dataTypes;

import edu.ozu.mapp.utils.Point;

public class Constraint
{
    public enum Duration {
        INF(-1);

        public int value;

        Duration(int t) { this.value = t; }

        public int getValue() {
            return value;
        }
    }

    public String agent_name;
    public Point location;
    public int   at_t;

    public Constraint(String key, Point point, int t)
    {
        agent_name = key;
        location = point;
        at_t     = t;
    }

    public Constraint(String key, Point point, String t) {
        this(key, point, t.equals("inf") ? -1 : Integer.parseInt(t));
    }

    public Constraint(Point point, int t) {
        this("", point, t);
    }

    public Constraint(String key, Point point) {
        this(key, point, -1); // INF
    }

    public Constraint(Point point) {
        this("", point, -1); // INF
    }

    @Override
    public boolean equals(Object that)
    {
        if (that instanceof Constraint)
            return (location.equals(((Constraint) that).location)) && (at_t == ((Constraint) that).at_t);

        return false;
    }

    @Override
    public String toString()
    {
        return String.format("(%s, %s):%s", location.x, location.y, at_t);
    }
}
