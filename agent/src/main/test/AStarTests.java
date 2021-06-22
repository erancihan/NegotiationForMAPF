import edu.ozu.mapp.utils.AStar;
import edu.ozu.mapp.utils.Globals;
import edu.ozu.mapp.utils.Point;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class AStarTests extends IntegrationTestSuite
{
    @Test
    public void test_short_path() throws Exception
    {
        HashMap<String, ArrayList<String>> constraints = new HashMap<>();

        List<String> received = new AStar().calculate(new Point(7, 5), new Point(7, 6), constraints, "16x16", 0);

        List<String> expected = new ArrayList<String>() {{
            add("7-5");
            add("7-6");
        }};
        assertTwoListEqual(expected, received);
    }

    @Test
    public void test_short_path_with_wait() throws Exception
    {
        Globals.MOVE_ACTION_SPACE_SIZE = 5;
        HashMap<String, ArrayList<String>> constraints = new HashMap<>();

        List<String> received = new AStar().calculate(new Point(7, 5), new Point(7, 6), constraints, "16x16", 0);

        List<String> expected = new ArrayList<String>() {{
            add("7-5");
            add("7-6");
        }};
        assertTwoListEqual(expected, received);
    }

    @Test
    public void test_short_path_with_wait2() throws Exception
    {
        Globals.MOVE_ACTION_SPACE_SIZE = 5;
        HashMap<String, ArrayList<String>> constraints = new HashMap<>();
        constraints.put("7-6", new ArrayList<String>() {{
            add("1");   // constraint time
        }});

        List<String> received = new AStar().calculate(new Point(7, 5), new Point(7, 6), constraints, "16x16", 0);

        List<String> expected = new ArrayList<String>() {{
            add("7-5");
            add("7-5");
            add("7-6");
        }};
        assertTwoListEqual(expected, received);
    }

    @Test
    public void test() throws Exception
    {
        Globals.MOVE_ACTION_SPACE_SIZE = 5;
        HashMap<String, ArrayList<String>> constraints = new HashMap<>();

        constraints.put("11-4", new ArrayList<String>() {{ add("6"); add("3"); }});
        constraints.put("11-3", new ArrayList<String>() {{ add("2"); }});
        constraints.put("10-4", new ArrayList<String>() {{ add("2"); add("5"); add("4"); }});
        constraints.put("10-3", new ArrayList<String>() {{ add("3"); }});
        constraints.put("10-2", new ArrayList<String>() {{ add("3"); }});
        constraints.put("10-1", new ArrayList<String>() {{ add("5"); }});
        constraints.put("10-0", new ArrayList<String>() {{ add("6"); }});
        constraints.put("5-1", new ArrayList<String>() {{ add("6"); }});
        constraints.put("6-1", new ArrayList<String>() {{ add("5"); add("6"); }});
        constraints.put("7-1", new ArrayList<String>() {{ add("5"); add("4"); }});
        constraints.put("8-1", new ArrayList<String>() {{ add("3"); add("4"); }});
        constraints.put("14-4", new ArrayList<String>() {{ add("4"); }});
        constraints.put("13-5", new ArrayList<String>() {{ add("6"); }});
        constraints.put("8-2", new ArrayList<String>() {{ add("3"); }});
        constraints.put("9-1", new ArrayList<String>() {{ add("2"); }});
        constraints.put("8-3", new ArrayList<String>() {{ add("2"); }});
        constraints.put("14-3", new ArrayList<String>() {{ add("3"); }});
        constraints.put("13-4", new ArrayList<String>() {{ add("5"); }});
        constraints.put("9-2", new ArrayList<String>() {{ add("2"); }});
        constraints.put("14-2", new ArrayList<String>() {{ add("2"); }});
        constraints.put("8-6", new ArrayList<String>() {{ add("6"); }});
        constraints.put("9-7", new ArrayList<String>() {{ add("5"); }});
        constraints.put("9-8", new ArrayList<String>() {{ add("6"); }});
        constraints.put("9-3", new ArrayList<String>() {{ add("3"); add("6"); add("2"); }});
        constraints.put("9-4", new ArrayList<String>() {{ add("4"); add("2"); add("3"); add("5"); }});
        constraints.put("9-5", new ArrayList<String>() {{ add("4"); add("3"); }});
        constraints.put("9-6", new ArrayList<String>() {{ add("5"); add("4"); }});

        List<String> received = new AStar().calculate(new Point(9, 3), new Point(2, 3), constraints, "16x16", 2);
    }
}
