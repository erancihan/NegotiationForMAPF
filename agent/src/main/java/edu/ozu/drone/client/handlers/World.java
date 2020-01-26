package edu.ozu.drone.client.handlers;

import edu.ozu.drone.utils.Globals;
import edu.ozu.drone.utils.Point;
import edu.ozu.drone.utils.Utils;
import redis.clients.jedis.ScanParams;
import redis.clients.jedis.ScanResult;

import java.util.ArrayList;

public class World {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(World.class);
    private static redis.clients.jedis.Jedis jedis = JedisConnection.getInstance();

    public static String[] list()
    {
        try {
            ArrayList<String> worlds = new ArrayList<>();
            ScanParams params = new ScanParams().match("world:*:");
            String cursor = ScanParams.SCAN_POINTER_START;
            boolean done = false;

            while (!done)
            {
                ScanResult<String> result = jedis.scan(cursor, params);
                worlds.addAll(result.getResult());

                cursor = result.getCursor();
                if (cursor.equals("0"))
                {
                    done = true;
                }
            }

            return worlds.toArray(new String[0]);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new String[0];
    }

    public static String[][] getFieldOfView(String worldID, String agentID)
    {
        ArrayList<String[]> agents = new ArrayList<>();
        Point loc = new Point(jedis.hget("world:" + worldID + ":map", "agent:" + agentID).split(":"));

        for (int i = 0; i < Globals.FIELD_OF_VIEW_SIZE; i++) {
            for (int j = 0; j < Globals.FIELD_OF_VIEW_SIZE; j++) {
                int axS = loc.x + (j - Globals.FIELD_OF_VIEW_SIZE / 2);
                int ayS = loc.y + (i + Globals.FIELD_OF_VIEW_SIZE / 2);

                String agent_key = jedis.hget("world:" + worldID + ":map", axS+":"+ayS);
                if (agent_key.length() > 0 && !(loc.x == axS && loc.y == ayS))
                { // key exists and it is not self
                    String path = jedis.hget("world:" + worldID + ":path", agent_key);
                    agents.add(new String[]{agent_key, axS+":"+ayS, path});
                }
            }
        }

        return agents.toArray(new String[0][3]);
    }

    public static int getTokenBalance(String worldID, String agentID)
    {
        try {
            String balance = jedis.hget("world:" + worldID + ":bank", "agent:" + agentID);

            return Integer.parseInt(balance);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }

    public static void doBroadcast(String wordlID, String agentID, String[] broadcast)
    {
        try {
            jedis.hset("world:" + wordlID + ":path", "agent:" + agentID, Utils.toString(broadcast, ","));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
