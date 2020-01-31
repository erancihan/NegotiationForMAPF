package edu.ozu.drone.client.handlers;

import edu.ozu.drone.utils.Globals;
import redis.clients.jedis.Jedis;

public class JedisConnection
{
    private static redis.clients.jedis.Jedis jedis;

    public static Jedis getInstance() {
        if (jedis == null)
            jedis = new Jedis(Globals.REDIS_HOST, Globals.REDIS_PORT);

        return jedis;
    }
}
