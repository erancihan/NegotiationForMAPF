package edu.ozu.mapp.utils.bid.generators;

import edu.ozu.mapp.utils.PathCollection;
import edu.ozu.mapp.utils.bid.BidSpaceGenerator;
import edu.ozu.mapp.utils.bid.MAPPBidSpaceGenerator;
import edu.ozu.mapp.utils.path.Node;
import edu.ozu.mapp.utils.path.Path;

import java.util.*;
import java.util.stream.Collectors;

@MAPPBidSpaceGenerator
public class PopLast extends BidSpaceGenerator
{
    private Node start;
    private Node cursor;
    private int depth;

    private PathCollection explored;
    private Stack<Node> stack;

    public PopLast() { }

    @Override
    public void init()
    {
        start = new Node(from, from.ManhattanDistTo(this.goal), time);
        depth = (int) from.ManhattanDistTo(this.goal) + 1;
        stack = new Stack<>();
        explored   = new PathCollection();
    }

    @Override
    public void prepare()
    {
        HashMap<String, Double> graph = new HashMap<>();
        PriorityQueue<Node> open = new PriorityQueue<>();
        List<Node> closed = new ArrayList<>();

        graph.put(this.start.point.key, 0.0);
        open.add(start);

        // explore neighbours
        while (!open.isEmpty())
        {
            Node current = open.remove();

            if (closed.contains(current)) continue;
            closed.add(current);

            if (current.path.size() + 1 == this.depth || current.point.equals(goal))
            {
                current.linkTo(current);
                cursor = current;
                return;
            }

            List<Node> neighbours = current.getNeighbours(goal, constraints, width, height);
            for (Node neighbour : neighbours)
            {
                if (closed.contains(neighbour))
                {
                    continue;
                }

                double d =
                        graph.get(current.point.key) +
                        Math.max(current.point.ManhattanDistTo(neighbour.point), 1.0)
                        ;
                if (d < graph.getOrDefault(neighbour.point.key, Double.MAX_VALUE))
                {
                    neighbour.dist = d + neighbour.dist;
                    graph.put(neighbour.point.key, d);

                    neighbour.linkTo(current);

                    try
                    {
                        open.add(neighbour.clone());
                    }
                    catch (CloneNotSupportedException e)
                    {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    @Override
    public Path next()
    {
        if (explored.isEmpty())
        {   // hasn't returned anything yet
            for (int i = 0; i < cursor.path.size(); i++) {
                //noinspection UseBulkOperation
                stack.add(cursor.path.get(i));
            }
            Path path = new Path(new ArrayList<>(stack).stream().map(node -> node.point).collect(Collectors.toList()));
            explored.add(path);

            return path;
        }

        // BEGIN : CALCULATE NEXT
        if (stack.isEmpty()) {
            return null;        // if stack is empty, return null
        }
        stack.pop();    // pop top most

        while (0 < stack.size() && stack.size() < depth) {
            Node current = stack.peek();
            if (current.point.equals(goal)) {
                break;          // target found, return this
            }

            Node next = getNextNode(current);
            if (next == null) {
                stack.pop();    // exhausted neighbourhood of current
            } else {
                stack.push(next);
            }
        }
        // END

        cursor.path = new LinkedList<>(stack);

        return new Path(cursor.path.stream().map(node -> node.point).collect(Collectors.toList()));
    }

    private Node getNextNode(Node current)
    {
        PriorityQueue<Node> neighbours = new PriorityQueue<>(current.getNeighbours(goal, constraints, width, height));
        while (!neighbours.isEmpty())
        {
            Node neighbour = neighbours.poll();

            Path next = new Path(new ArrayList<>(stack).stream().map(node -> node.point).collect(Collectors.toList()));
            next.add(neighbour.point);

            if (explored.contains(next)) continue;

            // unexplored, explore
            explored.add(next);

            return neighbour;
        }

        return null;
    }
}
