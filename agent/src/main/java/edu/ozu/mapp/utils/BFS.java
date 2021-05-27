package edu.ozu.mapp.utils;

import java.util.*;

public class BFS
{
    public int Max = Integer.MIN_VALUE;
    public int Min = Integer.MAX_VALUE;
    public ArrayList<Path> paths;
    public int max_paths = -1;

    private Point start;
    private Point goal;

    private int fov_r = -1;
    private boolean DEBUG = false;
    private int depth;
    private int Width = Integer.MAX_VALUE;
    private int Height = Integer.MAX_VALUE;

    public BFS(Point From, Point To, int FieldOfViewRadius, int depth, boolean IsDebug, int Width, int Height)
    {
        start = From;
        goal = To;

        paths = new ArrayList<Path>();

        DEBUG = IsDebug;
        fov_r = FieldOfViewRadius;
        this.depth = depth;
        this.Width = Width == 0 ? Integer.MAX_VALUE : Width;
        this.Height = Height == 0 ? Integer.MAX_VALUE : Height;
    }

    public BFS(Point From, Point To, int FieldOfViewRadius, int depth, int Width, int Height)
    {
        this(From, To, FieldOfViewRadius, depth, false, Width, Height);
    }

    public BFS(Point From, Point To, int FieldOfView, int depth)
    {
        this(From, To, FieldOfView, depth, false, Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    public BFS(Point From, Point To, int FieldOfView, int Width, int Height)
    {
        this(From, To, FieldOfView, (int) (From.ManhattanDistTo(To)*2), false, Width, Height);
    }

    public BFS(Point From, Point To, int FieldOfViewRadius)
    {
        this(From, To, FieldOfViewRadius, (int) (From.ManhattanDistTo(To)*2), false, Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    public BFS init()
    {
        GeneratePaths();

        return this;
    }

    public List<Path> GeneratePaths()
    {
        if (DEBUG) System.out.println(start + " ... " + goal + " :" + fov_r);

        Queue<Path> queue = new LinkedList<>();

        // add starting node to queue
        queue.add(new Path(){{add(start);}});

        while (!queue.isEmpty())
        {
            Path path = queue.remove(); // pop
            if (path.contains(goal) || path.size() >= depth)
            {   // Destination is reached || min path length requirement satisfied
                paths.add(path);

                // update max path length
                if (path.size() > Max) Max = path.size();
                if (path.size() < Min) Min = path.size();

                if (max_paths > 0 && paths.size() >= max_paths)
                    return paths;
                // get everything in field of view
//                continue;
            }

            Point current = path.getLast();

            // get neighbourhood
            List<Point> neighbourhood = GetNeighbourhood(current);
            for (Point neighbour : neighbourhood)
            {
                // if the neighbour is in the path
                // current is also in the path, so checks that too
                if (path.contains(neighbour)) continue;

                Path NewPath = new Path(path);
                NewPath.add(neighbour);
                queue.add(NewPath);
            }
        }

        return paths;
    }

    @SuppressWarnings("Duplicates")
    private List<Point> GetNeighbourhood(Point curr)
    {
        List<Point> hood = new ArrayList<>();

        if (curr.x > 0 && curr.x - 1 >= start.x - fov_r) {
            hood.add(new Point(curr.x - 1, curr.y));
        }
        if (curr.y > 0 && curr.y - 1 >= start.y - fov_r) {
            hood.add(new Point(curr.x, curr.y - 1));
        }
        if (curr.x < Width && curr.x + 1 <= start.x + fov_r) {
            hood.add(new Point(curr.x + 1, curr.y));
        }
        if (curr.y < Height && curr.y + 1 <= start.y + fov_r) {
            hood.add(new Point(curr.x, curr.y + 1));
        }
        hood.add(curr);  // we are in the hood too

        return hood;
    }

    public void SetMinimumPathLength(int i)
    {
        depth = i;
    }
}
