package edu.ozu.drone.client.handlers;

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
}
