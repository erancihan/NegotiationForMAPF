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
    private final boolean CAN_HOVER = false;
    @SuppressWarnings("FieldCanBeLocal")
    private final double INF = Double.MAX_VALUE;

    private int width, height;

    private int depth;
    private int time;

    private final PathCollection collection;
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

        collection = new PathCollection();
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

            if (current.path.size() + 1 == this.depth) {
                current.linkTo(current);
                cursor = current;
                return;
            }

            List<Node> neighbours = get_neighbours(current, current.time + 1);
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

    private List<Node> get_neighbours(Node current, int time)
    {
        List<Node> nodes = new ArrayList<>();

        for (int i = 0; i < 9; i++) {
            if (i % 2 == 0) continue;

            int x = (current.point.x + (i % 3) - 1);
            int y = (current.point.y + (i / 3) - 1);

            if ((x < 0 || x >= width) || (y < 0 || y >= height)) continue;

            Point next = new Point(x, y);
            if (constraints.containsKey(next.key))
            {
                if (
                    constraints.get(next.key).contains(String.valueOf(time)) ||
                    constraints.get(next.key).contains("inf")
                )
                    continue;
            }
            nodes.add(new Node(next, next.ManhattanDistTo(goal), time));
        }

        // add self for cyclic dep
        if (CAN_HOVER)
        {
            nodes.add(new Node(current.point, time));
        }

        return nodes;
    }

    public Point[] peek()
    {
        return cursor.path.stream().map(node -> node.point).toArray(Point[]::new);
    }

    public Point[] current()
    {
        return cursor.path.stream().map(node -> node.point).toArray(Point[]::new);
    }

    public Point[] next() // pop
    {
        if (stack.isEmpty())
        {   // hasn't returned anything yet
            for (int i = 0; i < cursor.path.size(); i++) {
                stack.add(cursor.path.get(i));
            }
            Path path = new Path(new ArrayList<>(stack).stream().map(node -> node.point).collect(Collectors.toList()));
            collection.add(path);
            explored.add(path);

            return new ArrayList<>(stack).stream().map(node -> node.point).toArray(Point[]::new);
        }

        // BEGIN : CALCULATE NEXT
        stack.pop();

        while (stack.size() < depth)
        {
            Node current = stack.peek();
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

        cursor.path = new LinkedList<>(stack);

        Path path = new Path(cursor.path.stream().map(node -> node.point).collect(Collectors.toList()));
        collection.add(path);

        return path.toArray(new Point[0]);
    }

    private Node getNextNode(Node current)
    {
        PriorityQueue<Node> neighbours = new PriorityQueue<>(get_neighbours(current, current.time + 1));
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

        BidSpace bid = new BidSpace(new Point(2, 2), new Point(10, 10), 5, new HashMap<>(), "11x11", 3);

        System.out.println("PEEK: " + Arrays.toString(bid.peek()));
        for (int i = 0; i < 20; i++) {
            System.out.println("NEXT: " + Arrays.toString(bid.next()));
        }
    }

//</editor-folds>
}
