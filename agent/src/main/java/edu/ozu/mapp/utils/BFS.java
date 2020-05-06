package edu.ozu.mapp.utils;

import java.util.*;

public class BFS
{
    @SuppressWarnings("Duplicates")
    public static void main(String[] args)
    {
        Point to = new Point(4, 4);
        BFS search = new BFS(new Point(2, 2), to, 6).init();

        PriorityQueue<Bid> bids = new PriorityQueue<>();
        for (Path path : search.paths)
        {
            if (path.contains(to))
                bids.add(
                        new Bid("AGENT_ID", path, (Integer x) -> (double) (1 - ((x - search.Min) / (search.Max - search.Min))))
                );
        }

        System.out.println(new ArrayList<>(bids));
    }

    public int Max = Integer.MIN_VALUE;
    public int Min = Integer.MAX_VALUE;
    public ArrayList<Path> paths;

    private Point _f;
    private Point _t;

    private int max_path_length = -1;
    private boolean DEBUG = false;

    public BFS(Point From, Point To, int MaxPathLength, boolean IsDebug)
    {
        _f = From;
        _t = To;

        paths = new ArrayList<Path>();

        DEBUG = IsDebug;
        if (MaxPathLength < 0) max_path_length = Math.abs(_f.x - _t.x) + Math.abs(_f.y - _t.y);
        else max_path_length = MaxPathLength;
    }

    public BFS(Point From, Point To, int MaxPathLength)
    {
        this(From, To, MaxPathLength, false);
    }

    public BFS(Point From, Point To, boolean IsDebug)
    {
        this(From, To, -1, IsDebug);
    }

    public BFS(Point From, Point To)
    {
        this(From, To, -1, false);
    }

    public BFS init()
    {
        GeneratePaths();

        return this;
    }

    public List<Path> GeneratePaths()
    {
        if (DEBUG) System.out.println(_f + " ... " + _t + " :" + max_path_length);

        Queue<Path> queue = new LinkedList<>();

        // add starting node to queue
        queue.add(new Path(){{add(_f);}});

        while (!queue.isEmpty())
        {
            Path CurrentPath = queue.remove(); // pop
            if (CurrentPath.size() > max_path_length || CurrentPath.contains(_t))
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

        if (curr.x > 0) {
            hood.add(new Point(curr.x - 1, curr.y));
        }
        if (curr.y > 0) {
            hood.add(new Point(curr.x, curr.y - 1));
        }
        hood.add(new Point(curr.x + 1, curr.y));
        hood.add(new Point(curr.x, curr.y + 1));
        hood.add(curr);  // we are in the hood too

        return hood;
    }
}
