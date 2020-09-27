package edu.ozu.mapp.utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class Path extends ArrayList<Point> implements Cloneable
{
    HashSet<Point> entries;

    public Path()
    {
        super();
        entries = new HashSet<>();
    }

    public Path(List<Point> asList)
    {
        super(asList);
        entries = new HashSet<>();
    }

    public Path(Path path)
    {
        super(path);
        entries = new HashSet<>(path.entries);
    }

    public Point getLast()
    {
        return get(size()-1);
    }

    @Override
    public boolean add(Point point)
    {
        entries.add(point);
        return super.add(point);
    }

    @Override
    public boolean contains(Object that)
    {
        return entries.contains(that);
    }

    public String[] toStringArray()
    {
        ArrayList<String> asStr = new ArrayList<>();
        for (Point p: this) asStr.add(p.key);

        return asStr.toArray(new String[0]);
    }
}