package edu.ozu.mapp.utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class PathCollection extends ArrayList<Path>
{
    HashSet<Path> entries;

    public PathCollection()
    {
        super();
        entries = new HashSet<>();
    }

    @Override
    public boolean add(Path path)
    {
        if (entries.add(path)) {
            return super.add(path);
        }

        return false;
    }

    @Override
    public boolean contains(Object that) {
        return super.contains(that);
    }

    public List<Path> toList() {
        return new ArrayList<>(this);
    }

    public Path getLast() {
        return this.get(this.size()-1);
    }
}
