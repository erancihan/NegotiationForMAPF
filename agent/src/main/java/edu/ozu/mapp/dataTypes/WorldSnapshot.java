package edu.ozu.mapp.dataTypes;

import edu.ozu.mapp.utils.Point;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class WorldSnapshot
{
    public HashSet<String>                      agent_keys;
    public HashMap<String, Point>               locations;
    public HashMap<String, ArrayList<Point>>    paths;
}
