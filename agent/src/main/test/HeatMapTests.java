import edu.ozu.mapp.utils.Globals;
import org.junit.Test;

import java.util.Arrays;

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

    public void heatmap_window_gen(int dims)
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
}
