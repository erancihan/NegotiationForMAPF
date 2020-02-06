package edu.ozu.drone.client.world;

import edu.ozu.drone.client.handlers.JedisConnection;
import edu.ozu.drone.utils.Globals;

import java.util.function.BiConsumer;

public class WorldHandler
{
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(WorldHandler.class);
    private static redis.clients.jedis.Jedis jedis = JedisConnection.getInstance();

    public static RedisListener createWorld(String WID, BiConsumer<String, String> callback)
    {
        if (jedis == null || !jedis.isConnected()) {
            return null;
        }

        if (jedis.exists(WID))
        {
            logger.error("«World already exists!»");
            return null;
        }
        logger.info("Creating " + WID + " ...");

        // create world
        jedis.hset(WID, "player_count", "0");
        jedis.hset(WID, "world_state", "0");
        jedis.hset(WID, "negotiation_count", "0"); // for negotiation state
        jedis.hset(WID, "move_action_count", "0"); // for move state
        jedis.hset(WID, "time_tick", "0"); // set time tick

        // subscribe(listen) to changes in world key
        return new RedisListener(Globals.REDIS_HOST, WID, callback);
    }
}
