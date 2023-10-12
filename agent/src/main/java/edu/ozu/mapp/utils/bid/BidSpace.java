package edu.ozu.mapp.utils.bid;

import edu.ozu.mapp.agent.client.helpers.FileLogger;
import edu.ozu.mapp.system.SystemExit;
import edu.ozu.mapp.utils.Globals;
import edu.ozu.mapp.utils.Point;
import edu.ozu.mapp.utils.path.Path;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class BidSpace
{
    public enum SearchStrategy
    {
        POP_LAST("edu.ozu.mapp.utils.bid.generators.PopLast"),

        NO_DEPTH_LIMIT("edu.ozu.mapp.utils.bid.generators.BFS"),
        BFS("edu.ozu.mapp.utils.bid.generators.BFS"),

        DFS("edu.ozu.mapp.utils.bid.generators.DFS")
        ;

        public String value;

        SearchStrategy(String name)
        {
            this.value = name;
        }
    }

    public static SearchStrategy searchStrategy             = SearchStrategy.POP_LAST;
    public static String         searchStrategyClassName    = "";

    private BidSpaceGenerator   generator               = null;
    private int                 invoke_count            = 0;

    private FileLogger fileLogger = null;

    public BidSpace() { }

    public void setFileLogger(FileLogger logger) {
        this.fileLogger = logger;
    }

    public void init
            (
                    Point   from,
                    Point   goal,
                    int     deadline,
                    HashMap<String, ArrayList<String>> constraints,
                    int     width,
                    int     height,
                    int     time
            )
    {
        if (Globals.BID_SEARCH_STRATEGY_OVERRIDE != null)
        {
            searchStrategy = Globals.BID_SEARCH_STRATEGY_OVERRIDE;
        }
        assert searchStrategy != null;
        searchStrategyClassName = searchStrategy.value;

        loadGenerator(findGenerators());

        // pass params
        generator.from          = from;
        generator.goal          = goal;
        generator.deadline      = deadline;
        generator.constraints   = constraints;
        generator.width         = width;
        generator.height        = height;
        generator.time          = time;

        // no constructor, call init
        generator.init();
    }

    public void init
            (
                    Point   from,
                    Point   goal,
                    int     deadline,
                    HashMap<String, ArrayList<String>> constraints,
                    String  dimensions,
                    int time
            )
    {
        String[] ds = dimensions.split("x");

        init
                (
                        from,
                        goal,
                        deadline,
                        constraints,
                        (ds.length == 2 && !ds[0].isEmpty() && !ds[0].equals("0")) ? Integer.parseInt(ds[0]) : Integer.MAX_VALUE,
                        (ds.length == 2 && !ds[1].isEmpty() && !ds[1].equals("0")) ? Integer.parseInt(ds[1]) : Integer.MAX_VALUE,
                        time
                );
    }

    public void init
            (
                    Point   from,
                    Point   goal,
                    HashMap<String, ArrayList<String>> constraints,
                    String  dimensions,
                    int     time
            )
    {
        init(from, goal, (int) (from.ManhattanDistTo(goal) + 1), constraints, dimensions, time);
    }

    private HashMap<String, Class<? extends BidSpaceGenerator>> findGenerators()
    {
        String[] lookup = new String[]{"edu.ozu.mapp.utils.bid.generators", "mapp.bid.generators"};
        HashMap<String, Class<? extends BidSpaceGenerator>> generators = new HashMap<>();

        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(true);
        scanner.addIncludeFilter(new AnnotationTypeFilter(MAPPBidSpaceGenerator.class));

        try
        {
            for (String basePackage : lookup)
            {
                for (BeanDefinition definition : scanner.findCandidateComponents(basePackage))
                {
                    generators
                        .put(
                            Objects.requireNonNull(definition.getBeanClassName()),
                            (Class<? extends BidSpaceGenerator>) Class.forName(definition.getBeanClassName())
                        );
                }
            }
        }
        catch (ClassNotFoundException ex)
        {
            ex.printStackTrace();
            System.exit(1);
        }

        return generators;
    }

    private void loadGenerator(HashMap<String, Class<? extends BidSpaceGenerator>> generators)
    {
        assert searchStrategyClassName != null;
        assert !searchStrategyClassName.isEmpty();

        try
        {
            generator = generators.get(searchStrategyClassName).getDeclaredConstructor().newInstance();
        }
        catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException ex)
        {
            ex.printStackTrace();
            System.exit(1);
        }

        assert generator != null;
    }

    public void prepare()
    {
        generator.prepare();
    }

    public Path next()
    {
        Path path = generator.next();

        try
        {
            if (path == null)
            {
                return null;
            }
            invoke_count++;
        }
        catch (NullPointerException exception)
        {
            System.err.println("nullptr " + generator.from + " -> " + generator.goal + " w/ " + generator.constraints + " @ t: " + generator.time + " | invoke:" + invoke_count);
            exception.printStackTrace();
            SystemExit.exit(500);
        }

//        if (this.fileLogger != null && Globals.LOG_BID_SPACE) {
//            fileLogger.LogBid();
//        }

        return path;
    }

    /**
     * Does exhaustive search of the bid space until
     * {@code next} returns null
     *
     * */
    public List<Path> all()
    {
        PriorityQueue<Path> paths = new PriorityQueue<>();

        for (int i = 0; i < Globals.MAX_BID_SPACE_POOL_SIZE; i++)
        {
            Path path = next();
            if (path != null)
            {
                paths.add(path);
            }
        }

        List<Path> resp = new ArrayList<>();
        while (!paths.isEmpty()) resp.add(paths.poll());

        return resp;
    }
}
