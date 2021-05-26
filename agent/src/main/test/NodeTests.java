import edu.ozu.mapp.utils.Globals;
import edu.ozu.mapp.utils.Point;
import edu.ozu.mapp.utils.path.Node;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class NodeTests extends IntegrationTestSuite {
    @Before
    public void before() {
        Globals.MOVE_ACTION_SPACE_SIZE = 5;
    }

    @Test
    public void test1() throws Exception {
        HashMap<String, ArrayList<String>> constraints = new HashMap<>();

        Node node = new Node(new Point(4, 6), 2);
        List<Node> nodes = node.getNeighbours(constraints, 16, 16);

        List<Node> expected = new ArrayList<>();
        expected.add(new Node(new Point(4, 5), 3)); // up
        expected.add(new Node(new Point(3, 6), 3)); // left
        expected.add(new Node(new Point(5, 6), 3)); // right
        expected.add(new Node(new Point(4, 7), 3)); // down
        if (Globals.MOVE_ACTION_SPACE_SIZE == 5)
        {
            expected.add(new Node(new Point(4, 6), 3)); // wait
        }

        assertTwoListEqual(expected, nodes);
    }

    @Test
    public void test2() throws Exception {
        HashMap<String, ArrayList<String>> constraints = new HashMap<>();
        constraints.put("4-6", new ArrayList<String>(){{ add("2"); }});

        Node node = new Node(new Point(4, 6), 2);
        List<Node> nodes = node.getNeighbours(constraints, 16, 16);

        List<Node> expected = new ArrayList<>();
        expected.add(new Node(new Point(4, 5), 3)); // up
        expected.add(new Node(new Point(3, 6), 3)); // left
        expected.add(new Node(new Point(5, 6), 3)); // right
        expected.add(new Node(new Point(4, 7), 3)); // down
        if (Globals.MOVE_ACTION_SPACE_SIZE == 5)
        {
            expected.add(new Node(new Point(4, 6), 3)); // wait
        }

        assertTwoListEqual(expected, nodes);
    }

    @Test
    public void test3() throws Exception {
        HashMap<String, ArrayList<String>> constraints = new HashMap<>();
        constraints.put("4-6", new ArrayList<String>(){{ add("3"); }});

        Node node = new Node(new Point(4, 6), 2);
        List<Node> nodes = node.getNeighbours(constraints, 16, 16);

        List<Node> expected = new ArrayList<>();
        expected.add(new Node(new Point(4, 5), 3)); // up
        expected.add(new Node(new Point(3, 6), 3)); // left
        expected.add(new Node(new Point(5, 6), 3)); // right
        expected.add(new Node(new Point(4, 7), 3)); // down

        assertTwoListEqual(expected, nodes);
    }

    @Test
    public void test4() throws Exception {
        HashMap<String, ArrayList<String>> constraints = new HashMap<>();
        constraints.put("3-6", new ArrayList<String>(){{ add("2"); }});

        Node node = new Node(new Point(4, 6), 2);
        List<Node> nodes = node.getNeighbours(constraints, 16, 16);

        List<Node> expected = new ArrayList<>();
        expected.add(new Node(new Point(4, 5), 3)); // up
        expected.add(new Node(new Point(3, 6), 3)); // left
        expected.add(new Node(new Point(5, 6), 3)); // right
        expected.add(new Node(new Point(4, 7), 3)); // down
        if (Globals.MOVE_ACTION_SPACE_SIZE == 5)
        {
            expected.add(new Node(new Point(4, 6), 3)); // wait
        }

        assertTwoListEqual(expected, nodes);
    }

    @Test
    public void test5() throws Exception {
        HashMap<String, ArrayList<String>> constraints = new HashMap<>();
        constraints.put("3-6", new ArrayList<String>(){{ add("3"); }});

        Node node = new Node(new Point(4, 6), 2);
        List<Node> nodes = node.getNeighbours(constraints, 16, 16);

        List<Node> expected = new ArrayList<>();
        expected.add(new Node(new Point(4, 5), 3)); // up
        expected.add(new Node(new Point(5, 6), 3)); // right
        expected.add(new Node(new Point(4, 7), 3)); // down
        if (Globals.MOVE_ACTION_SPACE_SIZE == 5)
        {
            expected.add(new Node(new Point(4, 6), 3)); // wait
        }

        assertTwoListEqual(expected, nodes);
    }

    @Test
    public void test6() throws Exception {
        HashMap<String, ArrayList<String>> constraints = new HashMap<>();
        constraints.put("4-6", new ArrayList<String>(){{ add("2"); }});
        constraints.put("3-6", new ArrayList<String>(){{ add("2"); }});

        Node node = new Node(new Point(4, 6), 2);
        List<Node> nodes = node.getNeighbours(constraints, 16, 16);

        List<Node> expected = new ArrayList<>();
        expected.add(new Node(new Point(4, 5), 3)); // up
        expected.add(new Node(new Point(3, 6), 3)); // left
        expected.add(new Node(new Point(5, 6), 3)); // right
        expected.add(new Node(new Point(4, 7), 3)); // down
        if (Globals.MOVE_ACTION_SPACE_SIZE == 5)
        {
            expected.add(new Node(new Point(4, 6), 3)); // wait
        }

        assertTwoListEqual(expected, nodes);
    }

    @Test
    public void test7() throws Exception {
        HashMap<String, ArrayList<String>> constraints = new HashMap<>();
        constraints.put("4-6", new ArrayList<String>(){{ add("2"); }});
        constraints.put("3-6", new ArrayList<String>(){{ add("3"); }});

        Node node = new Node(new Point(4, 6), 2);
        List<Node> nodes = node.getNeighbours(constraints, 16, 16);

        List<Node> expected = new ArrayList<>();
        expected.add(new Node(new Point(4, 5), 3)); // up
        expected.add(new Node(new Point(5, 6), 3)); // right
        expected.add(new Node(new Point(4, 7), 3)); // down
        if (Globals.MOVE_ACTION_SPACE_SIZE == 5)
        {
            expected.add(new Node(new Point(4, 6), 3)); // wait
        }

        assertTwoListEqual(expected, nodes);
    }

    @Test
    public void test8() throws Exception {
        HashMap<String, ArrayList<String>> constraints = new HashMap<>();
        constraints.put("4-6", new ArrayList<String>(){{ add("3"); }});
        constraints.put("3-6", new ArrayList<String>(){{ add("2"); }});

        Node node = new Node(new Point(4, 6), 2);
        List<Node> nodes = node.getNeighbours(constraints, 16, 16);

        List<Node> expected = new ArrayList<>();
        expected.add(new Node(new Point(4, 5), 3)); // up
        expected.add(new Node(new Point(5, 6), 3)); // right
        expected.add(new Node(new Point(4, 7), 3)); // down

        assertTwoListEqual(expected, nodes);
    }

    @Test
    public void test9() throws Exception {
        HashMap<String, ArrayList<String>> constraints = new HashMap<>();
        constraints.put("4-6", new ArrayList<String>(){{ add("3"); }});
        constraints.put("3-6", new ArrayList<String>(){{ add("3"); }});

        Node node = new Node(new Point(4, 6), 2);
        List<Node> nodes = node.getNeighbours(constraints, 16, 16);

        List<Node> expected = new ArrayList<>();
        expected.add(new Node(new Point(4, 5), 3)); // up
        expected.add(new Node(new Point(5, 6), 3)); // right
        expected.add(new Node(new Point(4, 7), 3)); // down

        assertTwoListEqual(expected, nodes);
    }
}
