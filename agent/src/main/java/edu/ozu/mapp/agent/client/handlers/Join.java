package edu.ozu.mapp.agent.client.handlers;

import edu.ozu.mapp.utils.Globals;
import edu.ozu.mapp.utils.Point;
import edu.ozu.mapp.utils.Utils;

import java.util.HashMap;

public class Join {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Join.class);
    private static redis.clients.jedis.Jedis jedis = JedisConnection.getInstance();

    public static void join(String worldID, String agentID, Point start, String broadcast)
    {
        try {
            // set map data
            jedis.hset("world:" + worldID + ":map", "agent:" + agentID, start.x + ":" + start.y);
            jedis.hset("world:" + worldID + ":map", start.x + ":" + start.y, "agent:" + agentID);

            // set broadcast data
            jedis.hset("world:" + worldID + ":path", "agent:" + agentID, broadcast);

            // set bank data
            jedis.hset("world:" + worldID + ":bank", "agent:" + agentID, String.valueOf(Globals.INITIAL_TOKEN_BALANCE));

            // register agent as ACTIVE
            jedis.sadd("world:" + worldID + ":active_agents", "agent:" + agentID);
        } catch (Exception e) {
            logger.error("an error happened while joining to world");
            e.printStackTrace();
        }
    }

    @Deprecated
    @SuppressWarnings("Duplicates")
    public static void __postJoin(String worldID, String agentID, Point start, String broadcast)
    {
        HashMap<String, Object> payload = new HashMap<>();
        payload.put("world_id", worldID);
        payload.put("agent_id", agentID);
        payload.put("agent_x", String.valueOf(start.x));
        payload.put("agent_y", String.valueOf(start.y));
        payload.put("broadcast", broadcast);

        String response = Utils.post("http://" + Globals.SERVER + "/join", payload);

        logger.info("__postJoin:" + worldID + "> " + response);
    }
}
