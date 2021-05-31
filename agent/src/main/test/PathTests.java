import edu.ozu.mapp.utils.Point;
import edu.ozu.mapp.utils.path.Path;
import org.junit.Test;

import java.util.HashSet;

public class PathTests extends IntegrationTestSuite {
    @Test
    public void test1() throws Exception {
        Path p1 = new Path("[0-1,0-2,0-3,0-4,0-4]");
        System.out.println(p1.string());

        System.out.println();
        Path p2 = new Path("[0-1,0-2,0-3,0-4]");
        p2.add(new Point(0, 4));
        System.out.println("equality test");
        System.out.println("p1: " + p1);
        System.out.println("p2: " + p2);
        System.out.println(
                "p1@" + Integer.toHexString(System.identityHashCode(p1.hashCode())) + " == p2@" + Integer.toHexString(System.identityHashCode(p2.hashCode())) +
                        " ? " + (p1.equals(p2))
        );

        System.out.println();
        System.out.println("HashSet insert test");
        HashSet<Path> set = new HashSet<>();
        System.out.println("add p1: " + set.add(p1));
        System.out.println("set contains p2? " + set.contains(p2));
        System.out.println("add p2: " + set.add(p2));
        System.out.println(set);

        System.out.println();
        Path p3 = new Path("[0-1,0-2,0-3,0-4]");
        p3.add(new Point(0, 3));
        System.out.println("p1: " + p1);
        System.out.println("p3: " + p3);
        System.out.println(
                "p1@" + Integer.toHexString(System.identityHashCode(p1.hashCode())) + " == p3@" + Integer.toHexString(System.identityHashCode(p3.hashCode())) +
                        " ? " + (p1.equals(p3))
        );
    }
}
