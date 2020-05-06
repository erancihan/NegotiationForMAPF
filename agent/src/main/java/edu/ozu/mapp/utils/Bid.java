package edu.ozu.mapp.utils;

import java.util.Arrays;
import java.util.function.Function;

public class Bid implements Comparable<Bid>
{
    public String agent_id;
    public Path path;
    public double utility;

    public Bid(String agent_id, Path path, Function<Integer, Double> utilityFunc)
    {
        this(agent_id, path, utilityFunc.apply(path.size()));
    }

    public Bid(String agent_id, Path path, double utility)
    {
        this.agent_id = agent_id;
        this.path = path;
        this.utility = utility;
    }

    public boolean equals(Bid o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        return agent_id.equals(o.agent_id) && path.equals(o.path);
    }

    @Override
    public String toString()
    {
        return "Bid{" + agent_id + ':' + Arrays.toString(path.toStringArray()) + ':' + utility + '}';
    }

    @Override
    public int compareTo(Bid that)
    {
        return 0;
    }
}
