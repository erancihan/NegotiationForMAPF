package edu.ozu.mapp.agent.client.world;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import edu.ozu.mapp.agent.client.WorldWatchSocketIO;
import edu.ozu.mapp.agent.client.helpers.FileLogger;
import edu.ozu.mapp.utils.Globals;
import edu.ozu.mapp.utils.Utils;
import redis.clients.jedis.Jedis;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class World
{
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(World.class);
    private static redis.clients.jedis.Jedis jedis;
    private FileLogger fl;
    private Gson gson = new Gson();

    private java.lang.reflect.Type messageMapType = new TypeToken<Map<String, String>>() {}.getType();

    private String WorldID;
    private Runnable OnLoopingStop;

    private boolean IsJedisOK = true;
    private boolean IsLooping = false;
    private boolean ShouldDeleteOnFinish = false;

    private long sim_start_time;
    private long sim_finish_time;
    private long sim_time_diff;

    private int prev_state_id = -1;
    private int notify_await_cycle = 0;
    private int negotiation_state_clock = 0;

    private ArrayList<Object[]> state_log = new ArrayList<>();
    private BiConsumer<Map<String, String>, ArrayList<Object[]>> LogDrawCallback;
    private Consumer<String> OnCurrentStateChange;
    private Runnable CanvasUpdateCallback;

    public World(boolean DeleteOnFinish)
    {
        ShouldDeleteOnFinish = DeleteOnFinish;
    }

    public World() {
        jedis = new Jedis(Globals.REDIS_HOST);

        try {
            jedis.connect();
        } catch (Exception ex) {
            logger.error("«can't connect to Redis»");
            IsJedisOK = false;

            ex.printStackTrace();
        }
    }

    public void Loop()
    {
        sim_start_time = System.nanoTime();
        logger.debug("SIM_START_TIME="+sim_start_time);
        state_log.add(new Object[]{"- SIM_START", new java.sql.Timestamp(System.currentTimeMillis())});

        IsLooping = true;
    }

    public void Stop()
    {
        IsLooping = false;
    }

    public void SetOnLoopingStop(Runnable func)
    {
        OnLoopingStop = func;
    }

    private void OnStateUpdate(Map<String, String> data)
    {
        int curr_state_id = Integer.parseInt(data.get("world_state"));

        if (curr_state_id == prev_state_id)
        {
            return; // handle only once
        }

        OnCurrentStateChange.accept(Globals.WORLD_STATES.get(curr_state_id).toString());

//        if (data.get("player_count").equals("0")) {
//            return; // do nothing if there are no players
//        }

        if (data.get("active_agent_count").equals("0"))
        {
            // do nothing if there are no active agents
            if (IsLooping)
            {
                IsLooping = false;
                sim_finish_time = System.nanoTime();

                if (OnLoopingStop != null)
                {
                    OnLoopingStop.run();
                }

                sim_time_diff = sim_finish_time - sim_start_time;

                logger.debug("SIM_FINISH_TIME="+sim_finish_time);
                logger.debug("SIM_DURATION_TIME:" + (sim_time_diff / 1E9));
                fl.LogWorldDone(data, (sim_time_diff / 1E9));

                long _t = System.currentTimeMillis();
                state_log.add(new Object[]{"- SIM_FINISH", new java.sql.Timestamp(_t)});
                state_log.add(new Object[]{"- SIM_DURATION: " + (sim_time_diff / 1E9) + " seconds", new java.sql.Timestamp(_t)});
            }

            return;
        }

        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        switch (curr_state_id)
        {
            case 0:
                state_log.add(new Object[]{"- end of join state", timestamp});

                logger.info("- end of join state");
                fl.LogWorldJoin(data);

                // join state, begin loop
                if (IsLooping) Step(curr_state_id);
                else {
                    prev_state_id = curr_state_id;
                }
                CanvasUpdateCallback.run();
                break;
            case 1:
                // collision check state, await 2 cycles for collision updates
                if (notify_await_cycle < 2)
                {
                    notify_await_cycle += 1;
                    jedis.hincrBy(WorldID, "time_tick", 1);
                    return; // return else
                }

                prev_state_id = curr_state_id; // update state

                state_log.add(new Object[]{"- collision check done", timestamp});

                logger.info("- collision check done");
                fl.LogWorldStateBroadcast(data, timestamp);

                // move to next state: 1 -> 2
                if (IsLooping) Step(curr_state_id);
                break;
            case 2:
                // clear notify await
                notify_await_cycle = 0;

                if (negotiation_state_clock == 0)
                {
                    fl.LogWorldStateNegotiate(data, timestamp, "BEGIN");
                }

                // negotiation state, do nothing until active negotiation_count is 0
                if (data.get("negotiation_count").equals("0"))
                {
                    prev_state_id = curr_state_id;

                    state_log.add(new Object[]{"- negotiations done", timestamp});

                    logger.info("- negotiations done");
                    fl.LogWorldStateNegotiate(data, timestamp, "DONE");

                    // move to next state: 2 -> 3
                    if (IsLooping) Step(curr_state_id);
                }

                negotiation_state_clock++;
                break;
            case 3:
                // clear negotiation_state_clock
                negotiation_state_clock = 0;

                // move state, wait for move_action_count
                // to match agent count, will indicate all agents took action
                if (data.get("move_action_count").equals(data.get("active_agent_count")))
                {
                    prev_state_id = curr_state_id;

                    state_log.add(new Object[]{"- movement complete", new java.sql.Timestamp(System.currentTimeMillis())});

                    logger.info("- movement complete");
                    fl.LogWorldStateMove(data, timestamp);

                    // clear move_action_count
                    jedis.hset(WorldID, "move_action_count", "0");
                    // move to next state: 3 -> 1
                    if (IsLooping) Step(curr_state_id);
                }
                break;
        }
    }

    public WorldWatchSocketIO Create(String WorldID, BiConsumer<Map<String, String>, ArrayList<Object[]>> callback)
    {
        return Create(WorldID, "0x0", callback);
    }

    public WorldWatchSocketIO Create(String WorldID, String Dimensions, BiConsumer<Map<String, String>, ArrayList<Object[]>> callback)
    {
        LogDrawCallback = callback;

        return Create(WorldID, Dimensions);
    }

    public WorldWatchSocketIO Create(String WorldID, String Dimensions)
    {
        if (!IsJedisOK)
        {
            logger.error("JEDIS connection is not OK!");
            return null;
        }

        this.WorldID = "world:" + WorldID + ":";
        fl = new FileLogger().CreateWorldLogger(WorldID);

        HashMap<String, Object> payload = new HashMap<>();
        payload.put("world_id", this.WorldID);
        payload.put("dimensions", Dimensions);

        payload.put("player_count", "0");
        payload.put("world_state", "0");
        payload.put("negotiation_count", "0");
        payload.put("move_action_count", "0");
        payload.put("time_tick", 0);

        Utils.post("http://localhost:5000/world/create", payload);
        fl.logWorldCreate(payload);

        return new WorldWatchSocketIO(
                this.WorldID,
                (message) -> {
                    Map<String, String> data = gson.fromJson(message, messageMapType);

                    LogDrawCallback.accept(data, state_log);

                    OnStateUpdate(data);
                }
        );
    }

    public void Step()
    {
        int curr_state = Integer.parseInt(jedis.hget(WorldID, "world_state"));
        Step(curr_state);
    }

    public void Step(int current_state)
    {
        switch (current_state) {
            case 0:
                // JOIN state, switch to COLLISION_CHECK state
            case 3:
                // MOVE state, switch to COLLISION_CHECK state
                jedis.hset(WorldID, "world_state", "1");
                break;
            case 1:
                // COLLISION_CHECK state, switch to NEGOTIATION state
                jedis.hset(WorldID, "world_state", "2");
                break;
            case 2:
                // NEGOTIATION state, switch to MOVE state
                jedis.hset(WorldID, "world_state", "3");
                break;
            default:
        }
    }

    public void Delete()
    {
        Delete(WorldID);
    }

    public static void Delete(String WorldID)
    {
        logger.info("Deleting " + WorldID + " ...");

        HashMap<String, Object> payload = new HashMap<>();
        payload.put("world_id", WorldID);

        Utils.post("http://localhost:5000/world/delete", payload);
    }

    public void SetCurrentStateChangeCallback(Consumer<String> callback)
    {
        this.OnCurrentStateChange = callback;
    }

    public void SetLogDrawCallback(BiConsumer<Map<String, String>, ArrayList<Object[]>> callback)
    {
        this.LogDrawCallback = callback;
    }

    public void Log(String str)
    {
        state_log.add(new Object[]{str, new java.sql.Timestamp(System.currentTimeMillis())});

        LogDrawCallback.accept(new HashMap<>(), state_log);
    }

    public void SetCanvasUpdateCallback(Runnable callback)
    {
        this.CanvasUpdateCallback = callback;
    }
}
