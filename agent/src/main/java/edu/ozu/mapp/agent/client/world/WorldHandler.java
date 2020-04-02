package edu.ozu.mapp.agent.client.world;

import edu.ozu.mapp.agent.client.handlers.JedisConnection;
import edu.ozu.mapp.utils.Globals;
import redis.clients.jedis.Jedis;

import java.util.Timer;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

public class WorldHandler
{
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(WorldHandler.class);
    private static redis.clients.jedis.Jedis jedis;

    public static RedisListener createWorld(String WID, BiConsumer<String, String> callback)
    {
        jedis = new Jedis(Globals.REDIS_HOST, Globals.REDIS_PORT);
        while (jedis == null) {
            logger.error("«Jedis is null!»");
            logger.info("connecting...");

            try {
                TimeUnit.MILLISECONDS.sleep(300);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        if (!jedis.isConnected()) {
            logger.error("«Jedis cannot connect!»");
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

    public static void deleteWorld(String WID)
    {
        logger.info("Deleting " + WID + " ...");

        jedis.del(WID, WID+"map", WID+"notify", WID+"path", WID+"session_keys", WID+"bank");
    }
}
