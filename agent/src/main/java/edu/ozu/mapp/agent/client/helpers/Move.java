package edu.ozu.mapp.agent.client.helpers;

import com.google.gson.Gson;
import edu.ozu.mapp.utils.Globals;
import edu.ozu.mapp.utils.JSONAgent;
import edu.ozu.mapp.utils.Point;
import edu.ozu.mapp.utils.Utils;

import java.util.HashMap;

public class Move {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Move.class);

    private static redis.clients.jedis.Jedis jedis = JedisConnection.getInstance();
    private static Gson gson = new Gson();

    public static JSONAgent move(String worldID, String agentID, Point point, String direction, String next_broadcast)
    {
        return __postMove(worldID, agentID, point, direction, next_broadcast);
    }

    @SuppressWarnings("Duplicates")
    public static JSONAgent __postMove(String worldID, String agentID, Point point, String direction, String next_broadcast)
    {
        int try_count = 0;

        // post localhost:3001/move payload:
        // direction -> {N, W, E, S}
        HashMap<String, Object> payload = new HashMap<>();
        payload.put("world_id", worldID);
        payload.put("agent_id", agentID);
        payload.put("agent_x", String.valueOf(point.x));
        payload.put("agent_y", String.valueOf(point.y));
        payload.put("direction", direction);
        payload.put("broadcast", next_broadcast);

        logger.debug("move payload:" + payload);

        JSONAgent response = null;
        try {
            String _resp = null;
            while (_resp == null && try_count < 2)
            {
                _resp = Utils.post("http://" + Globals.SERVER + "/move", payload);
                try_count++;

                if (_resp == null)
                {
                    Thread.sleep(100);
                }
            }
            response = gson.fromJson(_resp, JSONAgent.class);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // response should match with next path point in line
        logger.info("__postMove:" + response);

        return response;
    }
}
