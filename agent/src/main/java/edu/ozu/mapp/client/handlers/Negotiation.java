package edu.ozu.mapp.client.handlers;

import java.util.HashMap;
import java.util.Map;

public class Negotiation {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Negotiation.class);
    private static redis.clients.jedis.Jedis jedis = JedisConnection.getInstance();

    /**
     * Retrieves list of negotiation session IDs that agent will attend
     */
    public static String[] getSessions(String worldID, String agentID)
    {
        try {
            String session_list = jedis.hget("world:"+worldID+":notify", "agent:"+agentID);
            if (session_list == null)
                return new String[0];

            return session_list.split(",");
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new String[0];
    }

    public static String getActiveBid(String WORLD_ID, String AGENT_ID)
    {
        Map<String, String> sess = new HashMap<>();
        try {
            String session_id = jedis.hget("world:"+WORLD_ID+":notify", "agent:"+AGENT_ID);
            if (session_id.split(",").length > 1) {
                logger.error("something went wrong!");
                System.exit(1);
            }

            sess = jedis.hgetAll("negotiation:"+session_id);
            System.out.println(sess);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return sess.getOrDefault("bid:agent:"+AGENT_ID, "[]:0");
    }
}
