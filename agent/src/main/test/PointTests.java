import edu.ozu.mapp.utils.Point;
import org.junit.Test;

public class PointTests extends IntegrationTestSuite {
    @Test
    public void point_equals()
    {
        Point p1 = new Point(11, 6);
        Point p2 = new Point(11, 6);

        System.out.println(p1 + " == " + p2 + " ? " + p1.equals(p2));
    }
}
