package edu.ozu.mapp.utils;

import edu.ozu.mapp.utils.path.Node;

import java.util.*;
import java.util.stream.Collectors;

public class AStar {
    private static org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(AStar.class);

    public static void main(String[] args) {
        HashMap<String, ArrayList<String>> constraints = new HashMap<>();

        List<String> b = new AStar().calculate(new Point(7, 5), new Point(7, 6), constraints, "16x16", 7);
        System.out.println("b >: " + b);

        List<String> c = new AStar().calculate(new Point(7, 5), new Point(10, 15), constraints, "16x16", 7);
        System.out.println("c >: " + c);

        constraints.put("5-1", new ArrayList<String>(){{ add("3"); }});
        constraints.put("3-3", new ArrayList<String>(){{ add("3"); }});
        constraints.put("3-4", new ArrayList<String>(){{ add("2"); }});
        constraints.put("4-3", new ArrayList<String>(){{ add("4"); }});
        constraints.put("6-1", new ArrayList<String>(){{ add("2"); }});
        constraints.put("5-2", new ArrayList<String>(){{ add("4"); }});
        constraints.put("5-3", new ArrayList<String>(){{ add("5"); }});
        constraints.put("6-2", new ArrayList<String>(){{ add("5"); }});
        constraints.put("5-4", new ArrayList<String>(){{ add("6"); }});
        constraints.put("6-3", new ArrayList<String>(){{ add("6"); }});
        constraints.put("6-4", new ArrayList<String>(){{ add("9"); }});
        constraints.put("7-4", new ArrayList<String>(){{ add("8"); }});
        constraints.put("8-4", new ArrayList<String>(){{ add("7"); }});
        constraints.put("6-7", new ArrayList<String>(){{ add("10"); }});
        constraints.put("9-5", new ArrayList<String>(){{ add("7"); }});
        constraints.put("7-7", new ArrayList<String>(){{ add("11"); }});
        constraints.put("9-6", new ArrayList<String>(){{ add("8"); }});
        constraints.put("8-8", new ArrayList<String>(){{ add("9"); }});
        constraints.put("6-5", new ArrayList<String>(){{ add("8"); add("10"); }});
        constraints.put("7-5", new ArrayList<String>(){{ add("7"); add("9"); }});
        constraints.put("6-6", new ArrayList<String>(){{ add("9"); add("11"); }});
        constraints.put("8-5", new ArrayList<String>(){{ add("6"); add("8"); }});
        constraints.put("7-6", new ArrayList<String>(){{ add("8"); add("10"); }});
        constraints.put("8-6", new ArrayList<String>(){{ add("7"); add("9"); }});
        constraints.put("8-7", new ArrayList<String>(){{ add("8"); add("10"); }});

        List<String> d = new AStar().calculate(new Point(7, 5), new Point(10, 15), constraints, "16x16", 7);
        System.out.println("d >: " + d);
    }

    public List<String> calculate(Point start, Point dest, String[][] constraints_with_time, String dimensions, int t)
    {
        return calculateWithConstraints(start, dest, constraints_with_time, dimensions, t);
    }

    public List<String> calculateWithConstraints(Point start, Point dest, String[][] constraints_with_time, String dimensions, int t)
    {
        // parse constraints
        HashMap<String, ArrayList<String>> occupied_list = new HashMap<>();
        for (String[] constraint : constraints_with_time)
        {
            ArrayList<String> vals = occupied_list.getOrDefault(constraint[0], new ArrayList<>());
            vals.add(constraint[1]);
            occupied_list.put(constraint[0], vals);
        }

        return run(start, dest, occupied_list, dimensions, t);
    }

    /**
     * Calculate path with constraints
     *
     * @param start Starting point for calculation
     * @param dest Destination of calculation
     * @param constraints_with_time Constraint parameters with T timestamps
     * @param t Current time T of the world/agent
     *
     * @return List
     */
    public List<String> calculate(Point start, Point dest, HashMap<String, ArrayList<String>> constraints_with_time, String dimension, int t)
    {
        return run(start, dest, constraints_with_time, dimension, t);
    }

    public List<String> calculateWithConstraints(Point start, Point dest, HashMap<String, ArrayList<String>> constraints_with_time, int t)
    {
        return run(start, dest, constraints_with_time, "", t);
    }

    public List<String> calculate(Point start, Point dest)
    {
        return run(start, dest, new HashMap<>(), "", 0);
    }

    public List<String> calculate(Point start, Point dest, String Dimensions)
    {
        return run(start, dest, new HashMap<>(), Dimensions, 0);
    }

    //<editor-fold defaultstate="collapsed" desc="A-Star implementation">
    private List<String> run(Point start, Point goal, HashMap<String, ArrayList<String>> occupiedList, String Dimensions, int T)
    {
        if (start.equals(goal))
            return Collections.singletonList(start.key);

        double inf = Double.MAX_VALUE;

        String[] ds = Dimensions.split("x");
        int width  = (ds.length == 2 && !ds[0].isEmpty() && !ds[0].equals("0")) ? Integer.parseInt(ds[0]) : Integer.MAX_VALUE;
        int height = (ds.length == 2 && !ds[1].isEmpty() && !ds[1].equals("0")) ? Integer.parseInt(ds[1]) : Integer.MAX_VALUE;

        PriorityQueue<Node> open = new PriorityQueue<>();
        List<Node> closed = new ArrayList<>();
        HashMap<Node, Double> graph = new HashMap<>();

        graph.put(new Node(start, start.ManhattanDistTo(goal), T), 0.0);
        open.add( new Node(start, start.ManhattanDistTo(goal), T));

        while (!open.isEmpty())
        {
            Node current = open.remove();
            if (closed.contains(current))
            {
                continue;
            }

            closed.add(current);

            if (current.point.equals(goal))
            {
                return new Node(goal).linkTo(current).stream().map(item -> item.point.key).collect(Collectors.toList());
            }

            List<Node> neighbours = current.getNeighbours(occupiedList, width, height);
            for (Node neighbour : neighbours)
            {
                if (closed.contains(neighbour)) continue;

                // d <- g[current] + COST(current, neighbour, g)
                double d = graph.get(current) + Math.max(current.point.ManhattanDistTo(neighbour.point), 1);
                if (d < graph.getOrDefault(neighbour, inf))
                {
                    neighbour.dist = d + neighbour.point.ManhattanDistTo(goal);
                    graph.put(neighbour, d);

                    neighbour.linkTo(current);

                    try { open.add(neighbour.clone()); }
                    catch (CloneNotSupportedException e) { e.printStackTrace(); }
                }
            }
        }

        logger.error("!!! oh no, gonna return null");
        logger.error("!!! A*{" + start + ", " + goal + ", " + occupiedList + ", " + Dimensions + ", " + T + "}");

        return null;
    }

    //</editor-fold>
}

