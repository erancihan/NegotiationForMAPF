package edu.ozu.mapp.dataTypes;

import edu.ozu.mapp.utils.Point;

public class Constraint
{
    public Point location;
    public int   at_t;

    public Constraint(Point point, int t)
    {
        location = point;
        at_t     = t;
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
