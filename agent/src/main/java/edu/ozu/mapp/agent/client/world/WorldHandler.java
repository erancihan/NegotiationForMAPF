package edu.ozu.mapp.agent.client.world;

import edu.ozu.mapp.agent.client.WorldWatchSocketIO;
import edu.ozu.mapp.utils.Globals;
import edu.ozu.mapp.utils.Utils;

import java.util.HashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class WorldHandler
{
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(WorldHandler.class);
    private static redis.clients.jedis.Jedis jedis;

    public static RedisListener createWorld(String WID, BiConsumer<String, String> callback)
    {
        HashMap<String, Object> payload = new HashMap<>();
        payload.put("world_id", WID);
        payload.put("player_count", "0");
        payload.put("world_state", "0");
        payload.put("negotiation_count", "0");
        payload.put("move_action_count", "0");
        payload.put("time_tick", 0);

        Utils.post("http://localhost:5000/world/create", payload);

        // subscribe(listen) to changes in world key
        return new RedisListener(Globals.REDIS_HOST, WID, callback);
    }

    public static void deleteWorld(String WID)
    {
        logger.info("Deleting " + WID + " ...");

        HashMap<String, Object> payload = new HashMap<>();
        payload.put("world_id", WID);

        Utils.post("http://localhost:5000/world/delete", payload);
    }

    public void CreateWorld(String WID, Consumer<String> callback)
    {
        HashMap<String, Object> payload = new HashMap<>();
        payload.put("world_id", WID);
        payload.put("player_count", "0");
        payload.put("world_state", "0");
        payload.put("negotiation_count", "0");
        payload.put("move_action_count", "0");
        payload.put("time_tick", 0);

        Utils.post("http://localhost:5000/world/create", payload);

        WorldWatchSocketIO listener = new WorldWatchSocketIO(WID, callback);
    }
}
