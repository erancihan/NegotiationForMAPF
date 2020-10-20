package edu.ozu.mapp.agent.client.helpers;

import edu.ozu.mapp.utils.Globals;
import edu.ozu.mapp.utils.Point;
import edu.ozu.mapp.utils.Utils;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.ScanParams;
import redis.clients.jedis.ScanResult;

import java.util.ArrayList;

public class WorldHandler {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(WorldHandler.class);
    private static redis.clients.jedis.Jedis jedis = JedisConnection.getInstance();

    public String[] list()
    {
        try {
            Jedis jedis = new Jedis(Globals.REDIS_HOST, Globals.REDIS_PORT);

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

            jedis.close();

            return worlds.toArray(new String[0]);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new String[0];
    }

    public String[][] getFieldOfView(String worldID, String agentID)
    {
//        logger.info("get fov for: "+ worldID + " - " + agentID);
        Jedis jedis = new Jedis(Globals.REDIS_HOST, Globals.REDIS_PORT);
        ArrayList<String[]> agents = new ArrayList<>();
        Point loc = new Point(jedis.hget("world:" + worldID + ":map", "agent:" + agentID).split(":"));

        for (int i = 0; i < Globals.FIELD_OF_VIEW_SIZE; i++) {
            for (int j = 0; j < Globals.FIELD_OF_VIEW_SIZE; j++) {
                int axS = loc.x + (j - Globals.FIELD_OF_VIEW_SIZE / 2);
                int ayS = loc.y + (i - Globals.FIELD_OF_VIEW_SIZE / 2);

//                System.out.println("checking world:" + worldID + ":map" + " | " + axS+":"+ayS);
                String agent_key = jedis.hget("world:" + worldID + ":map", axS+":"+ayS);
                if (agent_key == null)
                    continue;
                if (agent_key.length() > 0 && !(loc.x == axS && loc.y == ayS))
                { // key exists and it is not self
                    String path = jedis.hget("world:" + worldID + ":path", agent_key);
                    agents.add(new String[]{agent_key, axS+":"+ayS, path});
                }
            }
        }
        jedis.close();

        return agents.toArray(new String[0][3]);
    }

    public int getTokenBalance(String worldID, String agentID)
    {
        try {
            Jedis jedis = new Jedis(Globals.REDIS_HOST, Globals.REDIS_PORT);
            String balance = jedis.hget("world:" + worldID + ":bank", "agent:" + agentID);
            jedis.close();

            if (balance == null || balance.isEmpty()) return 0;

            return Integer.parseInt(balance);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }

    public void doBroadcast(String wordlID, String agentID, String[] broadcast)
    {
        try {
            Jedis jedis = new Jedis(Globals.REDIS_HOST, Globals.REDIS_PORT);
            jedis.hset("world:" + wordlID + ":path", "agent:" + agentID, Utils.toString(broadcast, ","));
            jedis.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void leave(String WorldID, String AgentID)
    {
        try {
            Jedis jedis = new Jedis(Globals.REDIS_HOST, Globals.REDIS_PORT);
            jedis.srem("world:"+WorldID+":active_agents", "agent:"+AgentID);
            jedis.close();
            logger.debug("World@leave{world:"+WorldID+": , agent:"+AgentID+"}");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String GetDimensions(String WorldID)
    {
        try {
            Jedis jedis = new Jedis(Globals.REDIS_HOST, Globals.REDIS_PORT);
            String str = jedis.hget("world:"+WorldID+":", "dimensions");
            jedis.close();

            return str;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "0x0";
    }
}
