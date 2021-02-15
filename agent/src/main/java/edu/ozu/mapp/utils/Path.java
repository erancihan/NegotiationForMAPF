package edu.ozu.mapp.utils;

import edu.ozu.mapp.agent.client.helpers.ConflictCheck;
import edu.ozu.mapp.agent.client.helpers.ConflictInfo;

import java.util.*;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Path)) return false;
        if (!super.equals(o)) return false;
        Path points = (Path) o;
        return Objects.equals(entries, points.entries);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), entries);
    }

    public static void main(String[] args) {
        Path p1 = new Path("[0-1,0-2,0-3,0-4,0-4]");
        System.out.println(p1.string());

        System.out.println();
        Path p2 = new Path("[0-1,0-2,0-3,0-4]");
        p2.add(new Point(0, 4));
        System.out.println("equality test");
        System.out.println("p1: " + p1);
        System.out.println("p2: " + p2);
        System.out.println(
                "p1@" + Integer.toHexString(System.identityHashCode(p1.hashCode())) + " == p2@" + Integer.toHexString(System.identityHashCode(p2.hashCode())) +
                " ? " + (p1.equals(p2))
        );

        System.out.println();
        System.out.println("HashSet insert test");
        HashSet<Path> set = new HashSet<>();
        System.out.println("add p1: " + set.add(p1));
        System.out.println("set contains p2? " + set.contains(p2));
        System.out.println("add p2: " + set.add(p2));
    }
}
