package edu.ozu.mapp.utils.path;

import edu.ozu.mapp.agent.client.helpers.ConflictCheck;
import edu.ozu.mapp.agent.client.helpers.ConflictInfo;
import edu.ozu.mapp.utils.Point;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class Path extends ArrayList<Point> implements Cloneable, Comparable<Path>
{
    public HashMap<String, String> properties;
    HashSet<String> entries;

    public Path()
    {
        super();
        entries = new HashSet<>();
        properties = new HashMap<>();
    }

    public Path(List<Point> asList)
    {
        super(asList);
        entries = new HashSet<>();
        properties = new HashMap<>();
    }

    public Path(Path path)
    {
        super(path);
        entries = new HashSet<>(path.entries);
        properties = new HashMap<>();
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

        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), entries);
    }

    @Override
    public int compareTo(@NotNull Path that) {
        return Integer.compare(this.size(), that.size());
    }
}
