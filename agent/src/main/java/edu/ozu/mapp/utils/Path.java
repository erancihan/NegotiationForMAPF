package edu.ozu.mapp.utils;

import edu.ozu.mapp.agent.client.helpers.ConflictCheck;
import edu.ozu.mapp.agent.client.helpers.ConflictInfo;

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

    public Path(String[] path)
    {
        this(Arrays.stream(path).map(p -> new Point(p, "-")).collect(Collectors.toList()));
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
        ConflictInfo info = new ConflictCheck().check(this.toStringArray(), that.toStringArray());

        return info.hasConflict;
    }

    public String string() {
        return "[" + this.stream().map(point -> point.key).collect(Collectors.joining(",")) + "]";
    }

    public static void main(String[] args) {
        Path path = new Path("[0-1,0-2,0-3,0-4]");
        System.out.println(path.string());
    }
}
