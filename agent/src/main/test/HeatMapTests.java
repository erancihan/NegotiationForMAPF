import org.junit.Test;

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
}
