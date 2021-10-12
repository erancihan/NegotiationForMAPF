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

    public int[] heatmap_window_gen(int dims)
    {
        // center coordinates
        int center = (dims / 2) * (dims + 1);
        System.out.println(center);

        int[] heat_map = new int[dims * dims];
        for (int i = 0; i < dims; i++)
        {   // rows
            for (int j = 0; j < dims; j++)
            {   // cols
                System.out.printf("%s,%s ", i, j);
            }
            System.out.println();
        }
        System.out.println();
        for (int i = 0; i < dims; i++)
        {   // rows
            for (int j = 0; j < dims; j++)
            {   // cols
                System.out.printf("%3s ", (i * dims) + j);
            }
            System.out.println();
        }
        System.out.println();
        for (int i = 0; i < dims; i++)
        {   // rows
            for (int j = 0; j < dims; j++)
            {   // cols
                System.out.printf(
                        "%3s ",
                        dims - (Math.abs(i - (dims / 2)) + Math.abs(j - (dims / 2)))
                );
            }
            System.out.println();
        }
        System.out.println();
        for (int i = 0; i < dims; i++)
        {   // rows
            for (int j = 0; j < dims; j++)
            {   // cols
                heat_map[((i * dims) + j)] =
                        (dims - (Math.abs(i - (dims / 2)) + Math.abs(j - (dims / 2))));
            }
        }
        System.out.println(Arrays.toString(heat_map));
        System.out.println();
        for (int i = 0; i < (dims * dims); i++) {
            int x = ((i % dims) - (dims / 2));
            int y = ((i / dims) - (dims / 2));

            System.out.printf("%2s,%2s ", x, y);
            if ((i % dims) == (dims-1)) { System.out.println(); }
        }
        System.out.println();
        for (int i = 0; i < (dims * dims); i++) {
            int x = ((i % dims) - (dims / 2));
            int y = ((i / dims) - (dims / 2));

            System.out.printf("%5s ", heat_map[i]);
            if ((i % dims) == (dims-1)) { System.out.println(); } // pretty print
        }

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

    private double get_weight(Bid bid)
    {
        double weight = 0;

        for (Point point : bid.path)
        {   // add weights from heat map
            // each step taken is also +1 weight
            weight += 1 + HEAT_MAP.getOrDefault(point.key, 0.0);
        }

        return weight;
    }

    private HashMap<String, Double> HEAT_MAP = new HashMap<>();    // heat map
    @Test
    public void test_apply_heatmap()
    {
        Globals.BID_SEARCH_STRATEGY_OVERRIDE = BidSpace.SearchStrategy.BFS;

        int dims = 5;

        // get heat map
        int[] heat_map_weights = heatmap_window_gen(dims);

        // add another agent at point (4, 5)
        Point location = new Point(3, 4);

        for (int i = 0; i < (dims * dims); i++) {
            int x = location.x + ((i % dims) - (dims / 2));
            int y = location.y + ((i / dims) - (dims / 2));

            if (x < 0 || 10 <= x) continue;  // x : [bound_l, bound_r) | i.e. [0, 16) -> x : 0, 1, ... 15
            if (y < 0 || 10 <= y) continue;  // y : [bound_t, bound_b) | i.e. [0, 16) -> y : 0, 1, ... 15

            int w = heat_map_weights[i];
            // add weight to point increasingly
            HEAT_MAP.put(
                    String.format("%d-%d", x, y), // key
                    HEAT_MAP.getOrDefault(String.format("%d-%d", x, y), 0.0) + w // value
            );
        }
        System.out.println(HEAT_MAP);
        System.out.println();
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                System.out.printf("%5s ", 1 + HEAT_MAP.getOrDefault(String.format("%d-%d", j, i), 0.0));
            }
            System.out.println();
        }
        System.out.println();

        // generate bid space
        Point from = new Point(2, 2);
        Point to = DEST;

        BidSpace space = new BidSpace();
        space.init(
                from,
                to,
                100,
                new HashMap<>(),
                "10x10",
                0
        );
        space.prepare();

        double max = Double.MIN_VALUE;
        double min = Double.MAX_VALUE;
        List<Path> paths = new ArrayList<>();
        for (int i = 0; i < Globals.MAX_BID_SPACE_POOL_SIZE; i++)
        {
            Path next = space.next();
            if (next == null) break;
            if (next.size() == 0) break;

            double _max = next.size() + next.getLast().ManhattanDistTo(to);
            double _min = next.size() + next.getLast().ManhattanDistTo(to);

            if (_max > max) max = _max;
            if (_min < min) min = _min;

            List<String> rest = new AStar().calculate(next.getLast(), to, "10x10");
            for (int j = 1; j < rest.size(); j++) {
                next.add(new Point(rest.get(j), "-"));
            }

            paths.add(next);
        }

        PriorityQueue<Bid> bids = new PriorityQueue<>(Comparator.reverseOrder());
        for (Path path : paths)
        {
            bids.add(new Bid(
                    "AGENT_ID",
                    path,
                    UtilityFunction(new SearchInfo(max, min, path))
            ));
        }

        List<Bid> results = new ArrayList<>();
        while (!bids.isEmpty()) { results.add(bids.poll()); }

        PriorityQueue<Bid> bids1 = new PriorityQueue<>(Comparator.reverseOrder());
        PriorityQueue<Bid> bids2 = new PriorityQueue<>(Comparator.comparingDouble(this::get_weight));

        bids1.addAll(results);
        bids2.addAll(results);

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

            if (c == 30)
            {
                break;
            }
        }
    }
}
