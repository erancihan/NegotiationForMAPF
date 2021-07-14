package edu.ozu.mapp.utils.bid;

import edu.ozu.mapp.system.SystemExit;
import edu.ozu.mapp.utils.AStar;
import edu.ozu.mapp.utils.Globals;
import edu.ozu.mapp.utils.PathCollection;
import edu.ozu.mapp.utils.Point;
import edu.ozu.mapp.utils.path.Node;
import edu.ozu.mapp.utils.path.Path;

import java.util.*;
import java.util.stream.Collectors;

public class BidSpace
{
    public enum SearchStrategy
    {
        POP_LAST,
        NO_DEPTH_LIMIT, BFS
        ;
    }
    private final SearchStrategy strategy;

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

    public BidSpace(
        Point from,
        Point destination,
        int depth,
        HashMap<String, ArrayList<String>> constraints,
        String dimensions,
        int time,
        SearchStrategy strategy
    )
    {
        start = new Node(from, from.ManhattanDistTo(destination), time);

        goal  = destination;

        this.constraints = constraints;

        String[] ds = dimensions.split("x");
        width  = (ds.length == 2 && !ds[0].isEmpty() && !ds[0].equals("0")) ? Integer.parseInt(ds[0]) : Integer.MAX_VALUE;
        height = (ds.length == 2 && !ds[1].isEmpty() && !ds[1].equals("0")) ? Integer.parseInt(ds[1]) : Integer.MAX_VALUE;

        this.strategy = Globals.BID_SEARCH_STRATEGY_OVERRIDE == null ? strategy : Globals.BID_SEARCH_STRATEGY_OVERRIDE;
        this.depth = strategy == SearchStrategy.NO_DEPTH_LIMIT ? Integer.MAX_VALUE : depth;
        this.time  = time;

        invoke_count = 0;
        explored   = new PathCollection();
        stack = new Stack<>();
    }

    public BidSpace(
        Point from,
        Point destination,
        int depth,
        HashMap<String, ArrayList<String>> constraints,
        String dimensions,
        int time
    )
    {
        this(from, destination, depth, constraints, dimensions, time, SearchStrategy.POP_LAST);
    }

    public BidSpace(
        Point from,
        Point destination,
        HashMap<String, ArrayList<String>> constraints,
        String dimensions,
        int time,
        SearchStrategy strategy
    )
    {
        this(from, destination, (int) from.ManhattanDistTo(destination) + 1, constraints, dimensions, time, strategy);
    }

    public BidSpace(
            Point from,
            Point destination,
            HashMap<String, ArrayList<String>> constraints,
            String dimensions,
            int time
    )
    {
        this(from, destination, constraints, dimensions, time, SearchStrategy.POP_LAST);
    }

    public void prepare()
    {
        switch (this.strategy)
        {
            case BFS:
            case NO_DEPTH_LIMIT:
                __calculate_no_depth_limit();
                break;
            case POP_LAST:
            default:
                __calculate_pop_last();
        }
    }

    private void __calculate_pop_last()
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
        switch (this.strategy)
        {
            case BFS:
            case NO_DEPTH_LIMIT:
                path = __select_no_depth_limit();
                break;
            case POP_LAST:
            default:
                path = __select_pop_last();
        }

        try
        {
            if (path == null) {
                return null;
            }
            invoke_count++;
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

    private Path __select_pop_last() // pop
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
        while (!neighbours.isEmpty()) {
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

    private PriorityQueue<Node> Q = null;
    private void __calculate_no_depth_limit()
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

    private Path __select_no_depth_limit()
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

            // generate path from current to destination
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

    /**
     * Does exhaustive search of the bid space until
     * {@code next} returns null
     *
     * */
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
