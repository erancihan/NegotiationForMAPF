package edu.ozu.drone.client.handlers;

import com.google.gson.Gson;
import edu.ozu.drone.utils.Globals;
import edu.ozu.drone.utils.JSONAgent;
import edu.ozu.drone.utils.Point;
import edu.ozu.drone.utils.Utils;

import java.util.HashMap;

public class Move {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Move.class);
    private static redis.clients.jedis.Jedis jedis = JedisConnection.getInstance();
    private static Gson gson = new Gson();

    @SuppressWarnings("Duplicates")
    public static JSONAgent __postMove(String worldID, String agentID, Point point, String direction, String next_broadcast)
    {
        // post localhost:3001/move payload:
        // direction -> {N, W, E, S}
        HashMap<String, Object> payload = new HashMap<>();
        payload.put("world_id", worldID);
        payload.put("agent_id", agentID);
        payload.put("agent_x", String.valueOf(point.x));
        payload.put("agent_y", String.valueOf(point.y));
        payload.put("direction", direction);
        payload.put("broadcast", next_broadcast);

        JSONAgent response = gson.fromJson(Utils.post("http://" + Globals.SERVER + "/move", payload), JSONAgent.class);

        // response should match with next path point in line
        logger.info("__postMove:" + response);

        return response;
    }
}
