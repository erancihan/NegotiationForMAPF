package edu.ozu.mapp.dataTypes;

import edu.ozu.mapp.utils.Point;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class WorldSnapshot
{
    public int world_width;
    public int world_height;

    public HashSet<String>                      agent_keys;
    public HashMap<String, Point>               locations;
    public HashMap<String, ArrayList<Point>>    paths;

    public WorldSnapshot()
    {
        world_width     = 0;
        world_height    = 0;

        agent_keys = new HashSet<>();
        locations  = new HashMap<>();
        paths      = new HashMap<>();
    }

    @Override
    public String toString() {
        return
        "WorldSnapshot{" +
            "\n world_width=" + world_width + ", world_height=" + world_height + "," +
            "\n agent_keys=" + agent_keys + "," +
            "\n locations=" + locations + "," +
            "\n paths=" + paths + "" +
        "\n}"
        ;
    }
}
