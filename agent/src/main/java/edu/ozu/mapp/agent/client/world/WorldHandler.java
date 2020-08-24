package edu.ozu.mapp.agent.client.world;

import edu.ozu.mapp.agent.client.WorldWatchSocketIO;
import edu.ozu.mapp.utils.Utils;

import java.util.HashMap;
import java.util.function.Consumer;

public class WorldHandler
{
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(WorldHandler.class);
    private static redis.clients.jedis.Jedis jedis;

    public static void DeleteWorld(String WID)
    {
        logger.info("Deleting " + WID + " ...");

        HashMap<String, Object> payload = new HashMap<>();
        payload.put("world_id", WID);

        Utils.post("http://localhost:5000/world/delete", payload);
    }

    public WorldWatchSocketIO CreateWorld(String WID, Consumer<String> callback)
    {
        HashMap<String, Object> payload = new HashMap<>();
        payload.put("world_id", WID);
        payload.put("player_count", "0");
        payload.put("world_state", "0");
        payload.put("negotiation_count", "0");
        payload.put("move_action_count", "0");
        payload.put("time_tick", 0);

        Utils.post("http://localhost:5000/world/create", payload);

        return new WorldWatchSocketIO(WID, callback);
    }
}
