package edu.ozu.mapp.system;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class FoV {
    public ArrayList<Broadcast> broadcasts;

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
}
