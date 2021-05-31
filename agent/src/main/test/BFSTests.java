import edu.ozu.mapp.utils.BFS;
import edu.ozu.mapp.utils.path.Path;
import edu.ozu.mapp.utils.Point;
import org.junit.Test;

public class BFSTests extends IntegrationTestSuite {
    @Test
    public void test1() throws Exception {
        BFS search = new BFS(new Point(3, 3), new Point(5, 5), 5/2, 5, 5, 5).init();
        try {
            java.io.FileWriter writer = new java.io.FileWriter("output.txt");

            for (Path path: search.paths) writer.write(path.string() + System.lineSeparator());
        } catch (Exception e) {
            e.printStackTrace();
        }
        /*
        // TODO get Utility function in BFS to cut off search on a branch once utility is 0
        // TODO dont spawn after 0
        Point to = new Point(5, 5);

        long time = System.nanoTime();
        BFS search = new BFS(new Point(3, 3), to, 3, 8).init();
        time = System.nanoTime() - time;

        PriorityQueue<Bid> bids = new PriorityQueue<>();
        for (Path path : search.paths)
        {
            if (path.contains(to))
                bids.add(
                        new Bid("AGENT_ID", path, (Double x) -> 1 - ( Math.pow(x - search.Min, 2) / (search.Max - search.Min)))
                );
        }

        System.out.println("search exec time: "+ (time * 1E-9) + " seconds");
        System.out.println("number of items : " + bids.size());
        System.out.println("longest path    : " + search.Max);

        try {
            java.io.FileWriter writer = new java.io.FileWriter("output.txt");

            writer.write("search exec time: "+ (time * 1E-9) + " seconds" + System.lineSeparator());
            writer.write("number of items:" + bids.size() + System.lineSeparator());
            writer.write("longest path    : " + search.Max + System.lineSeparator());
            for (Bid bid: bids) writer.write(String.valueOf(bid));
        } catch (Exception e) {
            e.printStackTrace();
        }
        */
    }
}
