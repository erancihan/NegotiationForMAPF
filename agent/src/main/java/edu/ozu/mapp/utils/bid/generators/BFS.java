package edu.ozu.mapp.utils.bid.generators;

import edu.ozu.mapp.utils.AStar;
import edu.ozu.mapp.utils.PathCollection;
import edu.ozu.mapp.utils.Point;
import edu.ozu.mapp.utils.bid.BidSpaceGenerator;
import edu.ozu.mapp.utils.bid.MAPPBidSpaceGenerator;
import edu.ozu.mapp.utils.path.Node;
import edu.ozu.mapp.utils.path.Path;

import java.util.List;
import java.util.PriorityQueue;

@MAPPBidSpaceGenerator
public class BFS extends BidSpaceGenerator
{
    private Node                start;
    private Node                cursor;
    private PriorityQueue<Node> Q = null;
    private PathCollection      explored;

    public BFS() { }

    @Override
    public void init()
    {
        start       = new Node(from, from.ManhattanDistTo(this.goal), time);
        explored    = new PathCollection();
    }

    @Override
    public void prepare()
    {
        // set cursor to initial node
        try {
            this.cursor = start.clone();
            this.cursor.dist = (int) cursor.point.ManhattanDistTo(this.goal) + 1;

            this.Q = new PriorityQueue<>();
            this.Q.add(this.cursor);
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    @Override
    public Path next()
    {
        if (this.Q == null || this.Q.isEmpty()) return null;

        Path next_path = null;
        Node current;
        do {
            current = this.Q.remove();
            int current_dist_to_goal = (int) current.point.ManhattanDistTo(this.goal) + 1;
            // if current node path is not explored, explore
            PriorityQueue<Node> neighbours = new PriorityQueue<>(current.getNeighbours(this.goal, this.constraints, this.width, this.height));
            for (Node neighbour : neighbours)
            {
                neighbour.linkTo(current);

                int neigh_dist_to_goal = (int) neighbour.point.ManhattanDistTo(this.goal) + 1;
                if (neigh_dist_to_goal > current_dist_to_goal)
                {   // going away from goal
                    neighbour.dist = neighbour.path.size() + 1;
                }
                else
                {   // approaching closer to goal
                    neighbour.dist = neighbour.path.size() - 1;
                }

                if (this.explored.contains(neighbour.getPath()))
                {   // skip if path is explored
                    continue;
                }

                this.Q.add(neighbour);
            }

            // generate path from current node to destination
            // todo: explain further
            List<String> str_path = new AStar().calculate(current.point, this.goal, this.constraints, this.width + "x" + this.height, this.time);
            if (str_path == null)
            {   // return null if cant gen path
                return null;
            }
            for (String point : str_path) {
                current.getPath().add(new Point(point, "-"));
            }

            next_path = current.getPath();
        } while (this.explored.contains(current.getPath()));
        this.explored.add(current.getPath());

        return next_path;
    }
}
