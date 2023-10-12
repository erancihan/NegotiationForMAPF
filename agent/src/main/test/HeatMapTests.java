import edu.ozu.mapp.dataTypes.Constraint;
import edu.ozu.mapp.system.Broadcast;
import edu.ozu.mapp.utils.*;
import edu.ozu.mapp.utils.bid.BidSpace;
import edu.ozu.mapp.utils.path.Path;
import org.junit.Test;

import java.util.*;

public class HeatMapTests extends IntegrationTestSuite
{
    @Test
    public void test()
    {
        int a = 1;
        int b = 1;

        int[] ws = new int[]{
                1,  2,  1,
                2,  3,  2,
                1,  2,  1
        };
        for (int i = 0; i < 9; i++)
        {
            int dx = (i % 3) - 1;
            int dy = (i / 3) - 1;

            int x = a + dx;
            int y = b + dy;
            int w = ws[i];

            System.out.printf("%1d,%1d:%1d ", x, y, w);
            if (i == 2 || i == 5 || i == 8) System.out.println();
        }
    }

    public double[] heatmap_window_gen(int dims)
    {
        // center coordinates
        int center = (dims / 2) * (dims + 1);
        double[] heat_map = new double[dims * dims];
//        for (int i = 0; i < dims; i++)
//        {   // rows
//            for (int j = 0; j < dims; j++)
//            {   // cols
//                System.out.printf("%s,%s ", i, j);
//            }
//            System.out.println();
//        }
//        System.out.println();
//        for (int i = 0; i < dims; i++)
//        {   // rows
//            for (int j = 0; j < dims; j++)
//            {   // cols
//                System.out.printf("%3s ", (i * dims) + j);
//            }
//            System.out.println();
//        }
//        System.out.println();
//        for (int i = 0; i < dims; i++)
//        {   // rows
//            for (int j = 0; j < dims; j++)
//            {   // cols
//                System.out.printf(
//                        "%3s ",
//                        dims - (Math.abs(i - (dims / 2)) + Math.abs(j - (dims / 2)))
//                );
//            }
//            System.out.println();
//        }
//        System.out.println();
        for (int i = 0; i < dims; i++)
        {   // rows
            for (int j = 0; j < dims; j++)
            {   // cols
                int idx = ((i * dims) + j);
                heat_map[idx] =
                    (idx == center)
                        ? 999
                        : Math.max(0.0, ((double) (((dims / 2) + 1) - (Math.abs(i - (dims / 2)) + Math.abs(j - (dims / 2)))) / (double) ((dims / 2) + 1)))
                ;
            }
        }
//        System.out.println(Arrays.toString(heat_map));
//        System.out.println();
//        for (int i = 0; i < (dims * dims); i++) {
//            int x = ((i % dims) - (dims / 2));
//            int y = ((i / dims) - (dims / 2));
//
//            System.out.printf("%2s,%2s ", x, y);
//            if ((i % dims) == (dims-1)) { System.out.println(); }
//        }

        return heat_map;
    }

    @Test
    public void test_dim_5()
    {
        heatmap_window_gen(5);
    }

    @Test
    public void test_dim_7()
    {
        heatmap_window_gen(7);
    }

    Point DEST = new Point(5, 5);
    public double UtilityFunction(SearchInfo search)
    {
        // how far is the last point to destination
        double offset = 0;
        if (DEST != null) {
            offset = search.Path.getLast().ManhattanDistTo(DEST) * 1E-5;
        }

        return (1 - ((search.PathSize - search.MinPathSize) / (search.MaxPathSize - search.MinPathSize)) - offset);
    }

    private double get_w(Path path) {
        double w = 0.0;
        for (int i = 0; i < path.size(); i++)
        {
            Point point = path.get(i);
            w = w
                + 1.0
//                + OBSTACLES.getOrDefault(point.key, 0.0)    //  INF if obstacle
                + ((i < HEAT_MAPS.size())
                    ? HEAT_MAPS.get(i).getOrDefault(point.key, 0.0)
                    : 0.0
                )
            ;
        }

        return w;
    }

    private double calculate_heat_map_utility(double max_w, double min_w, double max_l, double min_l, Path path)
    {
//        double offset = 0; // offset by normalized path length
        double offset = (1 - ((max_l - path.size()) / (max_l - min_l))) * 1E-6; // offset by normalized path length

        return (1 - (Double.parseDouble(path.properties.get("weight")) - min_w) / (max_w - min_w)) - offset;
    }

    private final ArrayList<HashMap<String, Double>> HEAT_MAPS = new ArrayList<>();

    @Test
    public void test_apply_heatmap()
    {
        int dims = 5;
        int t = 0;
        int CAP = 999;

        // - make only center, opponent agent current position at T, a big number, 999
        //     to remove colliding path bids
        // - manuel, no constraint BID Space generation
        //     because we are adding constraints with Heat Map struct our selves
        // - apply iterative Heat Map
        //   - if no more Heat Map info, default cost to 1

        // todo: Bid Search +DEPTH_LIMIT
        // todo: skip own current location

        // get heat map
        double[] heat_map_weights = heatmap_window_gen(dims);

        for (int i = 0; i < (dims * dims); i++) {
            double w = heat_map_weights[i];
            if (w < CAP) {
                System.out.printf("%.2f ", w);
            } else {
                System.out.print(" INF ");
            }

            if ((i % dims) == (dims-1)) { System.out.println(); } // pretty print
        }
        System.out.println();

        // add another agent at point (3, 4)
        Broadcast broadcast_1 = new Broadcast();
        broadcast_1.locations.add(new Constraint("other_agent_1", new Point(4, 5), t)); // agents location
        broadcast_1.locations.add(new Constraint("other_agent_1", new Point(3, 5), t+1)); // agents location t+1
        broadcast_1.locations.add(new Constraint("other_agent_1", new Point(2, 5), t+2)); // agents location t+2
        broadcast_1.locations.add(new Constraint("other_agent_1", new Point(1, 5), t+3)); // agents location t+2

        Broadcast broadcast_2 = new Broadcast();
        broadcast_2.locations.add(new Constraint("other_agent_2", new Point(0, 0), t));
        broadcast_2.locations.add(new Constraint("other_agent_2", new Point(0, 1), t+1));
        broadcast_2.locations.add(new Constraint("other_agent_2", new Point(0, 2), t+2));
        broadcast_2.locations.add(new Constraint("other_agent_2", new Point(0, 3), t+3));

        ArrayList<Broadcast> broadcasts = new ArrayList<>();
        broadcasts.add(broadcast_1);
//        broadcasts.add(broadcast_2);

        for (Broadcast broadcast : broadcasts)
        {
            ArrayList<Constraint> constraints = broadcast.locations;
            for (int idx = 0; idx < constraints.size(); idx++)
            {
                Point location = broadcast.locations.get(idx).location;

                HashMap<String, Double> heat_map;
                if (HEAT_MAPS.size() <= idx) {
                    HEAT_MAPS.add(new HashMap<>());
                }
                heat_map = HEAT_MAPS.get(idx);

                for (int i = 0; i < (dims * dims); i++)
                {
                    int x = location.x + ((i % dims) - (dims / 2));
                    int y = location.y + ((i / dims) - (dims / 2));

                    if (x < 0 || 10 <= x) continue;  // x : [bound_l, bound_r) | i.e. [0, 16) -> x : 0, 1, ... 15
                    if (y < 0 || 10 <= y) continue;  // y : [bound_t, bound_b) | i.e. [0, 16) -> y : 0, 1, ... 15

                    double w = heat_map_weights[i];
                    // add weight to point increasingly
                    heat_map.put(
                            String.format("%d-%d", x, y), // key
                            heat_map.getOrDefault(String.format("%d-%d", x, y), 0.0) + w // value
                    );
                }
            }
        }

        System.out.println(HEAT_MAPS);
        System.out.println();

        for (int k = 0; k < HEAT_MAPS.size(); k++)
        {
            HashMap<String, Double> heat_map = HEAT_MAPS.get(k);
            System.out.println("t:"+k);
            for (int i = 0; i < 10; i++) {
                for (int j = 0; j < 10; j++) {
                    if (k == 0 && j == 2 && i == 2) { System.out.print(" 0  "); continue;}

                    double __w = 1 + heat_map.getOrDefault(String.format("%d-%d", j, i), 0.0);
                    if (__w < CAP) { System.out.printf("%.1f ", __w);}
                    else {           System.out.print (" X  "); }
                }
                System.out.println();
            }
            System.out.println();
        }

        // generate bid space
        Globals.BID_SEARCH_STRATEGY_OVERRIDE = BidSpace.SearchStrategy.BFS;
        Point from = new Point(2, 2);
        Point to = DEST;

        BidSpace space = new BidSpace();
        space.init(
                from,
                to,
                100,
                new HashMap<>(),    // constraints
                "10x10",
                t
        );
        space.prepare();

        // generate default bid space ordering
        // BEGIN
        double max_v1 = Double.MIN_VALUE;
        double min_v1 = Double.MAX_VALUE;

        double max_w = Double.MIN_VALUE;
        double min_w = Double.MAX_VALUE;

        List<Path> paths = new ArrayList<>();
        while (paths.size() < Globals.MAX_BID_SPACE_POOL_SIZE)
        {
            Path next = space.next();
            if (next == null) break;
            if (next.size() == 0) break;

            // add rest to path
            List<String> rest = new AStar().calculate(next.getLast(), to, "10x10");
            for (int j = 1; j < rest.size(); j++) {
                next.add(new Point(rest.get(j), "-"));
            }

            double len_v1 = next.size();
            if (len_v1 > max_v1) max_v1 = len_v1;
            if (len_v1 < min_v1) min_v1 = len_v1;

            double w = get_w(next);

            if (w >= CAP) {
                continue;
            }

            next.properties.put("weight", String.valueOf(w));
            if (w > max_w) max_w = w;
            if (w < min_w) min_w = w;

            paths.add(next);
        }

        PriorityQueue<Bid> bids1 = new PriorityQueue<>(Comparator.reverseOrder());
        for (Path path : paths)
        {
            bids1.add(new Bid(
                    "AGENT_ID",
                    path,
                    UtilityFunction(new SearchInfo(max_v1, min_v1, path))
            ));
        }

        PriorityQueue<Bid> bids2 = new PriorityQueue<>(Comparator.reverseOrder());
        for (Path path : paths)
        {
            bids2.add(new Bid(
                    "AGENT_ID",
                    path,
                    calculate_heat_map_utility(max_w, min_w, max_v1, min_v1, path)
            ));
        }

        // PRINT BID SPACES
        int c = 0;
        while (!bids1.isEmpty() && !bids2.isEmpty())
        {
            Bid b1 = bids1.poll();
            Bid b2 = bids2.poll();

            assert b1 != null;
            assert b2 != null;

            System.out.printf("%3s : %8f : %s \n", c, b1.utility, b1.path);
            System.out.printf("%3s : %8f : %s \n", c, b2.utility, b2.path);
            System.out.println();
            c++;

            if (c == 100)
            {
                break;
            }
        }
    }
}
