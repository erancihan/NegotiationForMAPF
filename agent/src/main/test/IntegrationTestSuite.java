import org.junit.Assert;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public abstract class IntegrationTestSuite {
    public <T> void assertTwoListEqual(Collection<T> expectedList, Collection<T> actualList) throws Exception
    {
        Assert.assertEquals(expectedList.size(), actualList.size());
        Set<T> set = new HashSet<>(actualList);
        for (T t : expectedList) {
            Assert.assertTrue(set.contains(t));
        }
        Set<T> set2 = new HashSet<>(expectedList);
        for (T t : actualList) {
            Assert.assertTrue(set2.contains(t));
        }
    }
}
