package edu.ozu.mapp.utils;

import java.util.*;

public class DFS
{
    public static void main(String[] args) {
        for (Point[] points : new DFS(new Point(0, 0), new Point(3, 3)).run())
            System.out.println(Arrays.toString(points));
    }

    private Point _f;
    private Point _t;

    private int max_recurse_depth;
    private ArrayList<Point[]> paths;

    public DFS(Point from, Point to) {
        _f = from;
        _t = to;

        paths = new ArrayList<>();
        max_recurse_depth = Math.abs(_f.x - _t.x) + Math.abs(_f.y - _t.y); // manhattan dist
    }

    public List<Point[]> run()
    {
        System.out.println(_f + " ... " + _t + " :" + max_recurse_depth);
        loop(_f, new ArrayList<>(), 0);

        return paths;
    }

    private void loop(Point curr, ArrayList<Point> history, int curr_depth)
    {
        if (curr.equals(_t))
        {   // i am target
            paths.add(history.toArray(new Point[0]));
            return;
        }

        if (curr_depth < max_recurse_depth)
        {
            // get hood
            Point[] neighbors = getNeighborhood(curr).toArray(new Point[0]);

            for (Point neighbor : neighbors)
            {
                if (neighbor.equals(curr)) continue;    // this is me already
                if (history.contains(neighbor)) continue;   // been there

                // update history
                ArrayList<Point> _h = new ArrayList<Point>(history);
                _h.add(neighbor);

                // spawn
                loop(neighbor, _h, curr_depth + 1);
            }
        }
    }

    private static List<Point> getNeighborhood(Point curr)
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
