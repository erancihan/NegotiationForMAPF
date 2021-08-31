import edu.ozu.mapp.utils.Globals;
import edu.ozu.mapp.utils.Point;
import edu.ozu.mapp.utils.bid.BidSpace;
import edu.ozu.mapp.utils.path.Path;
import org.junit.Test;

import java.util.*;

public class BidSpaceTests extends IntegrationTestSuite
{
    @Test
    public void test_dfs_search() throws CloneNotSupportedException
    {
        Globals.MAX_BID_SPACE_POOL_SIZE = 100;
        Globals.MOVE_ACTION_SPACE_SIZE = 4;
        Globals.BID_SEARCH_STRATEGY_OVERRIDE = BidSpace.SearchStrategy.DFS;

        Point f = new Point(2, 2);
        Point t = new Point(4, 4);

        BidSpace space = new BidSpace();
        space.init(f, t, new HashMap<>(), "11x11", 2);
        space.prepare();

        for (int i = 0; i < Globals.MAX_BID_SPACE_POOL_SIZE; i++)
        {
            Path next = space.next();
            System.out.printf("%3d / %3d > %s%n", i, Globals.MAX_BID_SPACE_POOL_SIZE, next);
        }
    }

    @Test
    public void test_get_no_depth_limit()
    {
        Globals.MAX_BID_SPACE_POOL_SIZE = 100;
        Globals.MOVE_ACTION_SPACE_SIZE = 4;
        Globals.BID_SEARCH_STRATEGY_OVERRIDE = BidSpace.SearchStrategy.NO_DEPTH_LIMIT;

        Point f2 = new Point(2, 2);
        Point t2 = new Point(4, 4);
        BidSpace space = new BidSpace();
        space.init(f2, t2, new HashMap<>(), "11x11", 3);
        space.prepare();

        PriorityQueue<Path> paths = new PriorityQueue<>();
        for (int i = 0; i < Globals.MAX_BID_SPACE_POOL_SIZE; i++) {
            Path next = space.next();
            if (next != null) { paths.add(next); }
        }

        for (int i = 0; !paths.isEmpty(); i++)
        {
            System.out.printf("%3d >: %s %s", i, paths.poll(), System.lineSeparator());
        }
    }

    @Test
    public void test_get_no_depth_limit_with_wait()
    {
        Globals.MOVE_ACTION_SPACE_SIZE = 5;
        Globals.BID_SEARCH_STRATEGY_OVERRIDE = BidSpace.SearchStrategy.NO_DEPTH_LIMIT;

        Point f2 = new Point(2, 2);
        Point t2 = new Point(4, 4);
        BidSpace space = new BidSpace();
        space.init(f2, t2, new HashMap<>(), "11x11", 3);
        space.prepare();

        PriorityQueue<Path> paths = new PriorityQueue<>();
        for (int i = 0; i < Globals.MAX_BID_SPACE_POOL_SIZE; i++) {
            Path next = space.next();
            if (next != null) { paths.add(next); }
        }

        for (int i = 0; !paths.isEmpty(); i++)
        {
            System.out.printf("%3d >: %s %s", i, paths.poll(), System.lineSeparator());
        }
    }

    @Test
    public void test_get_all()
    {
        Globals.MOVE_ACTION_SPACE_SIZE = 4;

        Point f2 = new Point(2, 2);
        Point t2 = new Point(4, 4);
        BidSpace space = new BidSpace();
        space.init(f2, t2, 5, new HashMap<>(), "11x11", 3);
        space.prepare();

        List<Path> paths = space.all();
        for (int i = 0; i < paths.size(); i++)
        {
            System.out.printf("%3d / %3d > %s%n", i, Globals.MAX_BID_SPACE_POOL_SIZE, paths.get(i));
        }
    }

    @Test
    public void test_gen_5() throws Exception
    {
        Globals.MOVE_ACTION_SPACE_SIZE = 4;

        Point f2 = new Point(2, 2);
        Point t2 = new Point(4, 4);
        BidSpace space = new BidSpace();
        space.init(f2, t2, 5, new HashMap<>(), "11x11", 3);

        List<Path> paths = new ArrayList<>();
        for (int i = 0; i < 5; i++)
        {
            Path next = space.next();
            paths.add(next);
        }

        List<Path> expected = new ArrayList<>();
        expected.add(new Path("[2-2,3-2,3-3,4-3,4-4]"));
        expected.add(new Path("[2-2,3-2,3-3,4-3,5-3]"));
        expected.add(new Path("[2-2,3-2,3-3,4-3,3-3]"));
        expected.add(new Path("[2-2,3-2,3-3,4-3,4-2]"));
        expected.add(new Path("[2-2,3-2,3-3,3-4,4-4]"));

        assertTwoListEqual(expected, paths);
    }

    @Test
    public void test_gen_5_with_wait() throws Exception
    {
        Globals.MOVE_ACTION_SPACE_SIZE = 5;

        Point f2 = new Point(2, 2);
        Point t2 = new Point(4, 4);
        BidSpace space = new BidSpace();
        space.init(f2, t2, 5, new HashMap<>(), "11x11", 3);

        List<Path> received = new ArrayList<>();
        for (int i = 0; i < 6; i++)
        {
            Path next = space.next();
            received.add(next);
        }

        List<Path> expected = new ArrayList<>();
        expected.add(new Path("[2-2,2-3,3-3,4-3,4-4]"));
        expected.add(new Path("[2-2,2-3,3-3,4-3,4-3]"));
        expected.add(new Path("[2-2,2-3,3-3,4-3,5-3]"));
        expected.add(new Path("[2-2,2-3,3-3,4-3,3-3]"));
        expected.add(new Path("[2-2,2-3,3-3,4-3,4-2]"));
        expected.add(new Path("[2-2,2-3,3-3,3-4,4-4]"));

        assertTwoListEqual(expected, received);
    }
}
