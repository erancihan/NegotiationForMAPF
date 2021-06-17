package edu.ozu.mapp.utils.bid;

import edu.ozu.mapp.system.SystemExit;
import edu.ozu.mapp.utils.PathCollection;
import edu.ozu.mapp.utils.Point;
import edu.ozu.mapp.utils.path.Node;
import edu.ozu.mapp.utils.path.Path;

import java.util.*;
import java.util.stream.Collectors;

public class BidSpace
{
    @SuppressWarnings("FieldCanBeLocal")
    private final double INF = Double.MAX_VALUE;

    private int width, height;

    private int depth;
    private int time;

    private int invoke_count;
    private final PathCollection explored;
    private final Stack<Node> stack;

    private Node cursor;

    private Node start;
    private Point goal;

    private final HashMap<String, ArrayList<String>> constraints;

    public BidSpace(Point from, Point destination, int depth, HashMap<String, ArrayList<String>> constraints, String dimensions, int time)
    {
        start = new Node(from, from.ManhattanDistTo(destination), time);

        goal  = destination;

        this.constraints = constraints;

        String[] ds = dimensions.split("x");
        width  = (ds.length == 2 && !ds[0].isEmpty() && !ds[0].equals("0")) ? Integer.parseInt(ds[0]) : Integer.MAX_VALUE;
        height = (ds.length == 2 && !ds[1].isEmpty() && !ds[1].equals("0")) ? Integer.parseInt(ds[1]) : Integer.MAX_VALUE;

        this.depth = depth;
        this.time  = time;

        invoke_count = 0;
        explored   = new PathCollection();
        stack = new Stack<>();

        calculate();
    }

    public BidSpace(Point from, Point destination, HashMap<String, ArrayList<String>> constraints, String dimensions, int time)
    {
        this(from, destination, (int) from.ManhattanDistTo(destination), constraints, dimensions, time);
    }

    private void calculate()
    {
        HashMap<Node, Double> graph = new HashMap<>();
        PriorityQueue<Node> open = new PriorityQueue<>();
        List<Node> closed = new ArrayList<>();

        graph.put(start, 0.0);
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
                if (closed.contains(neighbour)) continue;

                double d = graph.get(current) + Math.max(current.point.ManhattanDistTo(neighbour.point), 1.0);
                if (d < graph.getOrDefault(neighbour, INF))
                {
                    neighbour.dist = d + neighbour.dist;
                    graph.put(neighbour, d);

                    neighbour.linkTo(current);

                    try {
                        open.add(neighbour.clone());
                    } catch (CloneNotSupportedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public Path peek()
    {
        return new Path(cursor.path.stream().map(node -> node.point).collect(Collectors.toList()));
    }

    public Path current()
    {
        return peek();
    }

    public Path next()
    {
        Path path = null;
        try
        {
            if ((path = __next()) == null)
            {
                return null;
            }
            invoke_count++;
        }
        catch (EmptyStackException exception)
        {
            System.err.println("Stack is empty " + start.point + " -> " + goal + " w/ " + constraints + " @ t: " + time + " | invoke:" + invoke_count);
        }
        catch (NullPointerException exception)
        {
            System.err.println("nullptr " + start.point + " -> " + goal + " w/ " + constraints + " @ t: " + time + " | invoke:" + invoke_count);
            System.err.println("cursor: " + cursor);
            exception.printStackTrace();
            SystemExit.exit(500);
        }

        return path;
    }

    private Path __next() // pop
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
        if (stack.isEmpty())
        {   // if stack is empty, return null
            return null;
        }
        stack.pop();    // pop top most

        while (0 < stack.size() && stack.size() < depth)
        {
            Node current = stack.peek();
            if (current.point.equals(goal))
            {   // target found, return this
                break;
            }

            Node next = getNextNode(current);
            if (next == null)
            {   // exhausted neighbourhood of current
                stack.pop();
            }
            else
            {
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

    public List<Path> all()
    {
        List<Path> paths = new ArrayList<>();
        Path path = null;
        while ((path = next()) != null)
        {
            paths.add(path);
        }

        return paths;
    }
}
