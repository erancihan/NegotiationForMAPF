package edu.ozu.mapp.utils.bid.generators;

import edu.ozu.mapp.utils.bid.BidSpaceGenerator;
import edu.ozu.mapp.utils.bid.MAPPBidSpaceGenerator;
import edu.ozu.mapp.utils.path.Node;
import edu.ozu.mapp.utils.path.Path;

import java.util.HashSet;
import java.util.List;
import java.util.Stack;

@MAPPBidSpaceGenerator
public class DFS extends BidSpaceGenerator
{
    private Node            start;
    private int             search_depth = 0;
    private HashSet<Node>   discovered = null;

    public DFS() { }

    @Override
    public void init()
    {
        start = new Node(from, from.ManhattanDistTo(this.goal), time);
    }

    @Override
    public void prepare()
    {
        this.search_depth = (int) this.start.point.ManhattanDistTo(this.goal);
        this.discovered = new HashSet<>();
    }

    @Override
    public Path next()
    {
        Node result = null;

        int __initial = search_depth;
        for (int i = 0; i < 1; )
        {
            if (__initial + 3 <= search_depth)
            {   // break if search depth has increased too much
                break;
            }

            result = __select_dfs_process();
            if (result == null)
            {   // no results in current depth
                // increase search depth
                search_depth += 1;
                continue;
            }
            i++;
        }

        return (result == null) ? null : result.getPath();
    }

    private Node __select_dfs_process()
    {
        Node result = __select_dfs_search();
        if (result == null) { return null; }

        if (result.path.size() >= 1)
        {
            this.discovered.add(result);
        }

        Node copy = null;
        try
        {
            copy = result.clone(); // create a copy to work on
            copy.linkTo(copy);          // tie end point of the stack
        }
        catch (CloneNotSupportedException e)
        {
            e.printStackTrace();
            System.exit(418);
        }

        return copy;
    }

    private Node __select_dfs_search()
    {
        // create local discovered edges set
        // add previously traversed routes' edges as discovered
        HashSet<Node> mDiscovered = new HashSet<>(this.discovered);
        Stack<Node> S = new Stack<Node>();    // local stack

        try
        {
            S.add(start.clone());
        }
        catch (CloneNotSupportedException e)
        {
            e.printStackTrace();
            System.exit(365);
        }

        while (!S.isEmpty())
        {
            Node v = S.pop();       // get the top of the STACK
            if (v == null) break;   // stack is empty, break

            if (mDiscovered.contains(v)) continue;  // skip node if explored
            mDiscovered.add(v); // mark node discovered

            // path size limit has exceeded depth limit, pop & go back
            if (v.path.size() + 1 > search_depth) continue;

            // reached destination
            if (v.point.equals(goal))
            {
                return v;
            }

            List<Node> ws = v.getNeighbours(goal, constraints, width, height);
            for (Node w : ws)
            {
                // current edge has already been discovered
                if (mDiscovered.contains(w)) continue;
                w.linkTo(v);    // handle links

                S.add(w);       // add node to stack
            }
        }

        return null;
    }
}
