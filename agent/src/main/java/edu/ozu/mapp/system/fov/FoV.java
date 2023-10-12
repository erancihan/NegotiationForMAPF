package edu.ozu.mapp.system.fov;

import edu.ozu.mapp.dataTypes.Constraint;
import edu.ozu.mapp.system.Broadcast;
import edu.ozu.mapp.utils.Point;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class FoV {
    public ArrayList<Broadcast> broadcasts;
    public ArrayList<Point> obstacles;

    public FoV()
    {
        broadcasts = new ArrayList<>();
        obstacles = new ArrayList<>();
    }

    public void add(Broadcast broadcast) {
        broadcasts.add(broadcast);
    }

    public List<Broadcast> getBroadcastsSorted() {
        return broadcasts
                .stream()
                .sorted(Comparator.comparing(broadcast -> broadcast.agent_name))
                .collect(Collectors.toList())
                ;
    }

    @Override
    public String toString() {
        return "FoV{broadcasts=" + broadcasts + '}';
    }

    public String json() {
        return String.format("{\"broadcasts\":%s, \"obstacles\":%s}", broadcasts.toString(), obstacles.toString());
    }
}
