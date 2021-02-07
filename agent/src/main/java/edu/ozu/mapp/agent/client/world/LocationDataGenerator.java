package edu.ozu.mapp.agent.client.world;

import edu.ozu.mapp.agent.client.helpers.ConflictCheck;
import edu.ozu.mapp.agent.client.helpers.ConflictInfo;
import edu.ozu.mapp.utils.AStar;
import edu.ozu.mapp.utils.WorldConfig;
import edu.ozu.mapp.utils.Point;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;

public class LocationDataGenerator
{
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(LocationDataGenerator.class);
    private Random rng = new Random();

    private HashSet<String>     AgentStartLocations = new HashSet<>();
    private HashSet<String>     AgentDestinations   = new HashSet<>();
    private ArrayList<Point[]>  AgentLocationData   = new ArrayList<>();

    int number_of_expected_conflicts;
    WorldConfig world_data;

    public LocationDataGenerator(WorldConfig world_data, int number_of_expected_conflicts)
    {
        this.world_data = world_data;
        this.number_of_expected_conflicts   = number_of_expected_conflicts;
    }

    public ArrayList<Point[]> GenerateAgentLocationData(int width, int height)
    {
        logger.debug("Generating Agent Locations...");
        AgentLocationData = new ArrayList<>();

        int number_of_conflicts_remaining;
        ArrayList<Point[]> data;

        do {
            number_of_conflicts_remaining = number_of_expected_conflicts;
            data = new ArrayList<>();
            AgentStartLocations = new HashSet<>();  // flush
            AgentDestinations   = new HashSet<>();  // flush

            for (int index = 0; index < world_data.agent_count; index++)
            {
                boolean is_horizontal = rng.nextBoolean();
                int x_tresh = is_horizontal ? (int) Math.ceil(width * 0.3) : width;
                int y_tresh = is_horizontal ? height : (int) Math.ceil(height * 0.3);

                Point start;
                Point destination;

                do {
                    // pick a start
                    start = new Point(rng.nextInt(x_tresh), rng.nextInt(y_tresh));
                    // todo assert value is ok
                } while (
                    // there are agents starting from this point
                    AgentStartLocations.contains(start.key) ||
                    // there are agents within immediate close proximity for start | Premises Clear
                    !isPremisesClear(start.x, start.y)
                );
                AgentStartLocations.add(start.key);

                do {
                    // pick destination
                    destination = new Point(rng.nextInt(x_tresh) + (width - x_tresh), rng.nextInt(y_tresh) + (height - y_tresh));
                    // todo assert value is ok
                } while (
                    // there are agents going to this location
                    AgentDestinations.contains(destination.key) ||
                    // path length is not acceptable
                    !isPathLengthOk(start, destination)
                );
                AgentDestinations.add(destination.key);

                data.add(new Point[]{start, destination});
            }

            // get number of conflicts
            HashMap<String, String[]> paths = new HashMap<>();
            for (int i = 0; i < data.size() && number_of_conflicts_remaining > 0; i++)
            {
                Point[] a = data.get(i);
                String a_key = a[0].key + "-" + a[1].key;
                String[] a_path;

                if (paths.containsKey(a_key)) {
                    a_path = paths.get(a_key);
                } else {
                    a_path = new AStar().calculate(a[0], a[1]).toArray(new String[0]);
                    paths.put(a_key, a_path);
                }

                for (int j = i + 1; j < data.size() && number_of_conflicts_remaining > 0; j++)
                {
                    Point[] b = data.get(j);
                    String b_key = b[0].key + "-" + b[1].key;
                    String[] b_path;

                    if (paths.containsKey(b_key)) {
                        b_path = paths.get(b_key);
                    } else {
                        b_path = new AStar().calculate(b[0], b[1]).toArray(new String[0]);
                        paths.put(b_key, b_path);
                    }

                    ConflictInfo[] info = new ConflictCheck().GetAllConflicts(a_path, b_path);
                    if (info.length > 0)
                    {   // there are conflicts
                        logger.debug(String.format("Found %s conflict(s) between %s - %s", info.length, i, j));
                        number_of_conflicts_remaining -= info.length;
                    }
                }
            }
            logger.debug("# of conflicts left " + number_of_conflicts_remaining);
        } while (number_of_conflicts_remaining > 0);

        // append to Agent Location Data
        AgentLocationData.addAll(data);

        return AgentLocationData;
    }

    private boolean isPremisesClear(int _x, int _y)
    {
        return isPremisesClear(_x, _y, world_data.min_distance_between_agents);
    }

    private boolean isPremisesClear(int _x, int _y, int min_distance_between_agents)
    {
        // search the premises
        for (int i = 0; i < min_distance_between_agents; i++) {
            for (int j = 0; j < min_distance_between_agents; j++) {
                if (i == 0 && j == 0) continue; // self

                boolean is_occupied =
                    AgentStartLocations.contains((_x + i) + "-" + (_y + j)) ||
                    AgentStartLocations.contains((_x + i) + "-" + (_y - j)) ||
                    AgentStartLocations.contains((_x - i) + "-" + (_y + j)) ||
                    AgentStartLocations.contains((_x - i) + "-" + (_y - j));

                if (is_occupied) {
                    // Premise NOT Clear
                    return false;
                }
            }
        }

        return true;
    }

    private boolean isPathLengthOk(Point start, Point destination)
    {
        double dist = destination.ManhattanDistTo(start);

        // todo is min smaller than max????????

        return (
                // distance is less than max
                (world_data.max_path_len <= 0 || dist <= world_data.max_path_len)
                &&
                // distance is greater than min length
                (dist >= world_data.min_path_len)
        );
    }
}
