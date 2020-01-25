package edu.ozu.drone.client.handlers;

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
}
