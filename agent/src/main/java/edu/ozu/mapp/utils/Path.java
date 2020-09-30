package edu.ozu.mapp.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

public class Path extends ArrayList<Point> implements Cloneable
{
    HashSet<String> entries;

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

    public Path(String str)
    {
        this(
                Arrays.stream(str.replaceAll("([\\[\\]]*)", "").split(",")).map(p -> new Point(p, "-")).collect(Collectors.toList())
        );
    }

    public Point getLast()
    {
        return get(size()-1);
    }

    @Override
    public boolean add(Point point)
    {
        entries.add(point.key);
        return super.add(point);
    }

    @Override
    public boolean contains(Object that)
    {
        if (that instanceof Point) {
            return entries.contains(((Point) that).key);
        }
        return entries.contains(that);
    }

    public String[] toStringArray()
    {
        ArrayList<String> asStr = new ArrayList<>();
        for (Point p: this) asStr.add(p.key);

        return asStr.toArray(new String[0]);
    }

    public boolean HasConflictWith(Path that) {
        for (int i = 0; i < that.size() && i < this.size(); i++)
        {   // Vertex Conflict
            if (that.get(i).equals(this.get(i)))
                return true;
        }
        for (int i = 0; i + 1 < this.size() && i < that.size(); i++)
        {   // Swap conflict
            if (this.get(i + 1).equals(that.get(i)))
                return true;
        }
        return false;
    }
}
