package edu.ozu.mapp.agent.client.helpers;

import edu.ozu.mapp.agent.Agent;
import edu.ozu.mapp.utils.Globals;
import edu.ozu.mapp.utils.Point;
import edu.ozu.mapp.utils.Utils;

import java.util.HashMap;

public class Join {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Join.class);
    private static redis.clients.jedis.Jedis jedis = JedisConnection.getInstance();

    private Agent agent;

    public Join(Agent agent)
    {
        this.agent = agent;
    }

    public void join(String worldID)
    {
        String agentID = agent.AGENT_ID;
        String broadcast = agent.getBroadcast();
        Point start = agent.START;
        String init_token_c = String.valueOf(agent.initial_tokens);

        try {
            // set map data
            jedis.hset("world:" + worldID + ":map", "agent:" + agentID, start.x + ":" + start.y);
            jedis.hset("world:" + worldID + ":map", start.x + ":" + start.y, "agent:" + agentID);

            // set broadcast data
            jedis.hset("world:" + worldID + ":path", "agent:" + agentID, broadcast);

            // set bank data
            // set TOKEN for agent
            jedis.hset("world:" + worldID + ":bank", "agent:" + agentID, init_token_c);

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
