package edu.ozu.mapp.utils.bid;

import edu.ozu.mapp.utils.Point;
import edu.ozu.mapp.utils.path.Node;
import edu.ozu.mapp.utils.path.Path;

import java.util.ArrayList;
import java.util.HashMap;

public abstract class BidSpaceGenerator
{
    protected Point from;
    protected Point goal;
    public int deadline;
    protected HashMap<String, ArrayList<String>> constraints;
    protected int width;
    protected int height;
    protected int time;

    public BidSpaceGenerator() {}

    public abstract void init();

    public abstract void prepare();

    public abstract Path next();
}
