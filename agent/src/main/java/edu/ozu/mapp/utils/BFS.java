package edu.ozu.mapp.utils;

import java.util.*;

public class BFS
{
    @SuppressWarnings("Duplicates")
    public static void main(String[] args)
    {
        // TODO get Utility function in BFS to cut off search on a branch once utility is 0
        // TODO dont spawn after 0
        Point to = new Point(5, 5);

        long time = System.nanoTime();
        BFS search = new BFS(new Point(3, 3), to, 3, 8).init();
        time = System.nanoTime() - time;

        PriorityQueue<Bid> bids = new PriorityQueue<>();
        for (Path path : search.paths)
        {
            if (path.contains(to))
                bids.add(
                        new Bid("AGENT_ID", path, (Double x) -> 1 - ( Math.pow(x - search.Min, 2) / (search.Max - search.Min)))
                );
        }

        System.out.println("search exec time: "+ (time * 1E-9) + " seconds");
        System.out.println("number of items : " + bids.size());
        System.out.println("longest path    : " + search.Max);

        try {
            java.io.FileWriter writer = new java.io.FileWriter("output.txt");

            writer.write("search exec time: "+ (time * 1E-9) + " seconds" + System.lineSeparator());
            writer.write("number of items:" + bids.size() + System.lineSeparator());
            writer.write("longest path    : " + search.Max + System.lineSeparator());
            for (Bid bid: bids) writer.write(String.valueOf(bid));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int Max = Integer.MIN_VALUE;
    public int Min = Integer.MAX_VALUE;
    public ArrayList<Path> paths;

    private Point _f;
    private Point _t;

    private int FoV = -1;
    private boolean DEBUG = false;
    private int deadline;

    public BFS(Point From, Point To, int FieldOfView, int deadline, boolean IsDebug)
    {
        _f = From;
        _t = To;

        paths = new ArrayList<Path>();

        DEBUG = IsDebug;
        FoV = FieldOfView;
        this.deadline = deadline;
    }

    public BFS(Point From, Point To, int FieldOfView, int deadline)
    {
        this(From, To, FieldOfView, deadline, false);
    }

    public BFS(Point From, Point To, int FieldOfView)
    {
        this(From, To, FieldOfView, (int) (From.ManhattanDistTo(To)*2), false);
    }

    public BFS init()
    {
        GeneratePaths();

        return this;
    }

    public List<Path> GeneratePaths()
    {
        if (DEBUG) System.out.println(_f + " ... " + _t + " :" + FoV);

        Queue<Path> queue = new LinkedList<>();

        // add starting node to queue
        queue.add(new Path(){{add(_f);}});

        while (!queue.isEmpty())
        {
            Path CurrentPath = queue.remove(); // pop
            if (CurrentPath.contains(_t) || CurrentPath.size() >= deadline)
            {   // Destination is reached || path length cap reached
                paths.add(CurrentPath);

                // update max path length
                if (CurrentPath.size() > Max) Max = CurrentPath.size();
                if (CurrentPath.size() < Min) Min = CurrentPath.size();

                continue;
            }

            Point CurrentNode = CurrentPath.getLast();

            // get hood
            List<Point> hood = GetNeighborhood(CurrentNode);
            for (Point neighbor : hood)
            {
                // if the neighbor is in the path
                // current is also in the path, so checks that too
                if (CurrentPath.contains(neighbor)) continue;

                Path NewPath = new Path(CurrentPath);
                NewPath.add(neighbor);
                queue.add(NewPath);
            }
        }

        return paths;
    }

    @SuppressWarnings("Duplicates")
    private List<Point> GetNeighborhood(Point curr)
    {
        List<Point> hood = new ArrayList<>();

        if (curr.x > 0 && curr.x - 1 >= _f.x - FoV) {
            hood.add(new Point(curr.x - 1, curr.y));
        }
        if (curr.y > 0 && curr.y - 1 >= _f.y - FoV) {
            hood.add(new Point(curr.x, curr.y - 1));
        }
        if (curr.x + 1 <= _f.y + FoV) {
            hood.add(new Point(curr.x + 1, curr.y));
        }
        if (curr.y + 1 <= _f.y + FoV) {
            hood.add(new Point(curr.x, curr.y + 1));
        }
        hood.add(curr);  // we are in the hood too

        return hood;
    }
}
