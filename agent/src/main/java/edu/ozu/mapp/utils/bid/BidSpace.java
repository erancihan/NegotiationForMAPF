package edu.ozu.mapp.utils.bid;

import edu.ozu.mapp.utils.Path;
import edu.ozu.mapp.utils.PathCollection;
import edu.ozu.mapp.utils.Point;
import edu.ozu.mapp.utils.path.Node;

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
            path = __next();
            invoke_count++;
        }
        catch (EmptyStackException exception)
        {
            System.err.println("Stack is empty " + start.point + " -> " + goal + " w/ " + constraints + " @ t: " + time + " | invoke:" + invoke_count);
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
        stack.pop();

        while (stack.size() < depth)
        {
            Node current = stack.peek();

            if (current.point.equals(goal)) break;

            Node next = getNextNode(current);
            if (next == null) {
                // exhausted neighbourhood of current
                stack.pop();
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

/** ================================================================================================================ **/
//<editor-folds desc="TESTS" defaultstate="collapsed">

    public static void main(String[] args)
    {
        HashSet<Node> set = new HashSet<>();
        set.add(new Node(new Point(1, 2), 2));
        set.add(new Node(new Point(1, 2), 3));
        System.out.println(set);
        set.add(new Node(new Point(1, 2), 2));
        System.out.println(set);
        System.out.println();

        Point f1 = new Point(4, 6);
        Point t1 = new Point(5, 6);
        BidSpace bs1 = new BidSpace(f1, t1, 5, new HashMap<>(), "16x16", 10);

        for (int i = 0; i < 50; i++) {
            System.out.println("NEXT: " + bs1.next());
        }

        System.out.println();

        Point f2 = new Point(2, 2);
        Point t2 = new Point(4, 4);
        BidSpace bs2 = new BidSpace(f2, t2, 5, new HashMap<>(), "11x11", 3);

        double max = Double.MIN_VALUE;
        double min = Double.MAX_VALUE;
        for (int i = 0; i < 5; i++)
        {
            Path next = bs2.next();
            System.out.println("NEXT:" + next);

            double _max = next.size() + next.getLast().ManhattanDistTo(t2);
            double _min = next.size() + next.getLast().ManhattanDistTo(t2);

            if (_max > max) max = _max;
            if (_min < min) min = _min;
        }
        System.out.println("MIN: "+ min);
        System.out.println("MAX: "+ max);
    }

//</editor-folds>
}
