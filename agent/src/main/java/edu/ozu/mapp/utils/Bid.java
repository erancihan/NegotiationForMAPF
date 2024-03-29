package edu.ozu.mapp.utils;

import edu.ozu.mapp.utils.path.Path;

import java.util.Arrays;
import java.util.function.Function;

public class Bid implements Comparable<Bid>
{
    public String agent_id;
    public Path path;
    public double utility;

    public Bid(String agent_id, Path path, Function<Double, Double> utilityFunc)
    {
        this(agent_id, path, utilityFunc.apply((double) path.size()));
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

    public String toJSON()
    {
        return String.format(
                "{\"utility\":%.16f,\"props\":%s,\"path\":%s,\"agent_id\":\"%s\"}",
                utility,
                path.properties,
                Arrays.toString(path.toStringArray()),
                agent_id
        );
    }


    @Override
    public String toString()
    {
        return "\nBid{" + agent_id + ':' + Arrays.toString(path.toStringArray()) + ':' + utility + '}';
    }

    @Override
    public int compareTo(Bid that)
    {
        return Double.compare(this.utility, that.utility);
    }
}
