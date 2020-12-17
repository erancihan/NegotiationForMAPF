package edu.ozu.mapp.system;

import edu.ozu.mapp.agent.client.AgentClient;
import edu.ozu.mapp.agent.client.AgentHandler;
import edu.ozu.mapp.agent.client.helpers.FileLogger;
import edu.ozu.mapp.utils.Globals;
import edu.ozu.mapp.utils.JSONWorldWatch;
import edu.ozu.mapp.utils.Point;
import edu.ozu.mapp.utils.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class WorldOverseer
{
    private static WorldOverseer instance;

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(WorldOverseer.class);
    private FileLogger flogger;

    protected   String              WorldID = "";
    protected   int                 width   = 0;
    protected   int                 height  = 0;
    private     int                 active_agent_c = 0;
    private     Globals.WorldState  curr_state;
    private     Globals.WorldState  prev_state;

    private boolean IsLooping = false;

    private ScheduledExecutorService service;

    private ConcurrentHashMap<String, AgentClient>  clients;
    /**
     * K: Agent name
     * V: Agent's broadcasted path
     * */
    protected ConcurrentHashMap<String, String[]> broadcasts;
    protected ConcurrentHashMap<String, Integer>  bank_data;
    protected ConcurrentHashMap<String, String>   agent_to_point;
    protected ConcurrentHashMap<String, String>   point_to_agent;
    protected ConcurrentSkipListSet<String>       active_agents;

    private MovementHandler     movement_handler;
    private NegotiationOverseer negotiation_overseer;

    private ArrayList<Object[]> state_log = new ArrayList<>();

    private long SIM_LOOP_START_TIME;
    private long SIM_LOOP_FINISH_TIME;
    private long SIM_LOOP_DURATION;
    private int TIME;

    DATA_LOG_DISPLAY log_payload;

    private ConcurrentHashMap<String, String> FLAG_JOINS;
    private ConcurrentHashMap<String, String> FLAG_COLLISION_CHECKS;
    private ConcurrentHashMap<String, String> FLAG_NEGOTIATIONS_DONE;
    private ConcurrentHashMap<String, String> FLAG_INACTIVE;

    private Consumer<DATA_LOG_DISPLAY>    UI_LogDrawCallback;
    private Consumer<String>                                        UI_StateChangeCallback;
    private Runnable                                                UI_CanvasUpdateHook;
    private Runnable                                                UI_LoopStoppedHook;

    private WorldOverseer()
    {
        clients             = new ConcurrentHashMap<>();
        curr_state          = Globals.WorldState.JOIN;
        prev_state          = Globals.WorldState.NONE;
        movement_handler    = MovementHandler.getInstance();
        negotiation_overseer = NegotiationOverseer.getInstance();
        negotiation_overseer.world_log_callback = this::Log;
        negotiation_overseer.log_payload_hook = this::LogNegotiation;

        broadcasts     = new ConcurrentHashMap<>();
        bank_data      = new ConcurrentHashMap<>();
        agent_to_point = new ConcurrentHashMap<>();
        point_to_agent = new ConcurrentHashMap<>();
        active_agents  = new ConcurrentSkipListSet<>();

        log_payload    = new DATA_LOG_DISPLAY();

        FLAG_JOINS            = new ConcurrentHashMap<>();
        FLAG_COLLISION_CHECKS = new ConcurrentHashMap<>();
        FLAG_NEGOTIATIONS_DONE = new ConcurrentHashMap<>();
        FLAG_INACTIVE         = new ConcurrentHashMap<>();

        TIME = 0;
    }

    public static WorldOverseer getInstance()
    {
        if (instance == null)
        {
            synchronized (WorldOverseer.class)
            {
                if (instance == null)
                {
                    instance = new WorldOverseer();
                }
            }
        }

        return instance;
    }

    public void Create(String world_id, int width, int height)
    {
        WorldID     = world_id;
        this.width  = width;
        this.height = height;

        flogger = new FileLogger().CreateWorldLogger(world_id);
        movement_handler.SetWorldReference(this);

        HashMap<String, Object> payload = new HashMap<>();
        payload.put("world_id",     world_id);
        payload.put("dimensions",   width + "x" + height);
        flogger.logWorldCreate(payload);
    }

    public void SetOnLoopingStop(Runnable callback)
    {
        UI_LoopStoppedHook = callback;
    }

    public void SetLogDrawCallback(Consumer<DATA_LOG_DISPLAY> callback)
    {
        UI_LogDrawCallback = callback;
    }

    public void SetCurrentStateChangeCallback(Consumer<String> callback)
    {
        UI_StateChangeCallback = callback;
    }

    public void SetCanvasUpdateCallback(Runnable callback)
    {
        UI_CanvasUpdateHook = callback;
    }

    public void Run()
    {
        service = Executors.newScheduledThreadPool(clients.size() + 1);
        service.scheduleAtFixedRate(this::run, 0, 250, TimeUnit.MILLISECONDS);

        for (String agent_name : clients.keySet())
        {
            AgentClient client = clients.get(agent_name);
            service.scheduleAtFixedRate(() -> client.UpdateState(get_current_state(client.GetAgentName())), 100, 500, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Logic loop function that checks state and
     * handles actions
     * */
    private void run()
    {
        Map<String, String> data = new HashMap<>();
        data.put("World ID", WorldID);
        data.put("Dimensions", width+"x"+height);
        data.put("World State", curr_state.toString());
        data.put("World TIME", String.valueOf(TIME));
        data.put("Active Agent Count", String.valueOf(active_agent_c));
        data.put("Active Negotiation Count", String.valueOf(negotiation_overseer.ActiveCount()));
        data.put("Cumulative Negotiation Count", String.valueOf(negotiation_overseer.CumulativeCount()));

        UI_CanvasUpdateHook.run();

        if (curr_state == prev_state)
        {   // HANDLE ONCE
            return;
        }

        UI_StateChangeCallback.accept(curr_state.toString());

        if (active_agent_c == 0 && IsLooping)
        {   // there are no active agents left!
            IsLooping = false;
            SIM_LOOP_FINISH_TIME = System.nanoTime();

            if (UI_LoopStoppedHook != null) { UI_LoopStoppedHook.run(); }

            SIM_LOOP_DURATION = SIM_LOOP_FINISH_TIME - SIM_LOOP_START_TIME;

            logger.debug("SIM_LOOP_FINISH_TIME=" + SIM_LOOP_FINISH_TIME);
            logger.debug("SIM_LOOP_DURATION   =" + (SIM_LOOP_DURATION / 1E9));

            long _t = System.currentTimeMillis();
            state_log.add(new Object[]{"- SIM_LOOP_FINISH_TIME", new java.sql.Timestamp(_t)});
            state_log.add(new Object[]{String.format("- SIM_LOOP_DURATION %s seconds", (SIM_LOOP_DURATION / 1E9)), new java.sql.Timestamp(_t)});

            return;
        }

        switch (curr_state)
        {
            case JOIN:
                UI_CanvasUpdateHook.run();

                if (FLAG_JOINS.size() == active_agent_c)
                {   // ALL REGISTERED AGENTS ARE DONE JOINING

                    logger.info("- END OF JOIN STATE");

                    prev_state = curr_state;

                    if (IsLooping) { Step(); }
                }

                break;
            case BROADCAST:
                if (FLAG_COLLISION_CHECKS.size() == active_agent_c)
                {   // BROADCASTS DONE
                    logger.info("- BROADCASTS ARE DONE");

                    prev_state = curr_state;

                    if (IsLooping) { Step(); }
                }

                break;
            case NEGOTIATE:
                if (FLAG_NEGOTIATIONS_DONE.size() == active_agent_c)
                {   // NEGOTIATIONS ARE COMPLETE
                    logger.info("- NEGOTIATIONS ARE COMPLETE");

                    FLAG_NEGOTIATIONS_DONE.clear();

                    prev_state = curr_state;

                    if (IsLooping) { Step(); }
                }

                break;
            case MOVE:
                if (active_agent_c == movement_handler.size())
                {
                    movement_handler
                        .ProcessQueue(() -> CompletableFuture.runAsync(() -> {
                            logger.info("- MOVES ARE COMPLETE");

                            prev_state = curr_state;

                            // Reset data
                            FLAG_COLLISION_CHECKS.clear();
                            FLAG_NEGOTIATIONS_DONE.clear();

                            TIME++;
                            log_payload.LogAgentLocations(agent_to_point);
                            log_payload.LogWorldTime(TIME);

                            if (IsLooping) { Step(); }
                        }));
                }

                break;
            default:
        }

        log_payload.world_data = data;
        log_payload.world_log  = state_log;

        UI_LogDrawCallback.accept(log_payload);
    }

    private JSONWorldWatch get_current_state(String agent_name)
    {
        // TODO
        JSONWorldWatch data = new JSONWorldWatch();
        data.world_state = curr_state.key;
        data.fov_size    = Globals.FIELD_OF_VIEW_SIZE;
        data.fov         = GetAgentFoV(agent_name);
        data.time_tick   = 0;

        return data;
    }

    private String[][] GetAgentFoV(String agent_name)
    {
        ArrayList<String[]> agents = new ArrayList<>();
        Point loc = new Point(agent_to_point.get(agent_name), "-");

        for (int i = 0; i < Globals.FIELD_OF_VIEW_SIZE; i++) {
            for (int j = 0; j < Globals.FIELD_OF_VIEW_SIZE; j++)
            {
                int x = loc.x + (j - Globals.FIELD_OF_VIEW_SIZE / 2);
                int y = loc.y + (i - Globals.FIELD_OF_VIEW_SIZE / 2);

                if (x == loc.x && y == loc.y) continue;

                String agent_key = point_to_agent.getOrDefault(x + "-" + y, "");
                if (!agent_key.isEmpty())
                {
                    agents.add(new String[]{agent_key, x + "-" + y, Utils.toString(broadcasts.get(agent_key), ",")});
                }
            }
        }

        return agents.toArray(new String[0][3]);
    }

    public void Step()
    {
        Step(curr_state.key);
    }

    public void Step(int state_key)
    {
        prev_state = curr_state;

        switch (state_key)
        {
            case 0:
                // JOIN state, switch to COLLISION_CHECK state
            case 3:
                // MOVE state, switch to COLLISION_CHECK | BROADCAST state
                logger.debug("switching to BROADCAST");
                curr_state = Globals.WorldState.BROADCAST;
                break;
            case 1:
                // COLLISION_CHECK | BROADCAST state, switch to NEGOTIATION state
                logger.debug("switching to NEGOTIATE");
                curr_state = Globals.WorldState.NEGOTIATE;
                break;
            case 2:
                // NEGOTIATION state, switch to MOVE state
                logger.debug("switching to MOVE");
                curr_state = Globals.WorldState.MOVE;
                break;
            default:
        }
    }

    public void Loop()
    {
        SIM_LOOP_START_TIME = System.nanoTime();

        logger.debug("SIM_LOOP_START_TIME=" + SIM_LOOP_START_TIME);
        Log("- SIM_START");

        IsLooping = true;

        if (curr_state == prev_state)
        {   // already run once, proceed to next step immediately
            Step();
        }
    }

    public void Stop()
    {
        IsLooping = false;
    }

    /**
     * Move function
     * */
    public void Register(AgentClient client)
    {
        client.SetJoinCallback(this::Join);
        client.SetOnCollisionCheckDoneCallback(this::OnCollisionCheckDone);
//        client.SetGetNegotiationContractHook();
        client.SetGetNegotiationsHook(this::GetNegotiations);
        client.SetJoinNegotiationSession(this::JoinNegotiationSession);
        client.SetNegotiatedCallback(this::Negotiated);
        client.SetMoveCallback(this::Move);
        client.SetLeaveHook(this::Leave);

        clients.put(client.GetAgentName(), client);

        logger.info(String.format("Agent %s has Registered", client.GetAgentName()));
        Log(String.format("Agent %s has Registered", client.GetAgentName()));

        client.WORLD_HANDLER_JOIN_HOOK();

        active_agent_c++;
    }

    public synchronized String[] Join(DATA_REQUEST_PAYLOAD_WORLD_JOIN payload)
    {
        FLAG_JOINS.put(payload.AGENT_NAME, "");

        // set map data
        agent_to_point.put(payload.AGENT_NAME, payload.LOCATION.key);
        point_to_agent.put(payload.LOCATION.key, payload.AGENT_NAME);

        // set broadcast data
        broadcasts.put(payload.AGENT_NAME, payload.BROADCAST);

        // set bank data, TOKEN for agent
        bank_data.put(payload.AGENT_NAME, payload.INIT_TOKEN_C);

        // register agent as ACTIVE
        active_agents.add(payload.AGENT_NAME);

        log_payload.LogAgentLocations(agent_to_point);

        return new String[]{WorldID, width+"x"+height};
    }

    /**
     * @function OnCollisionCheckDone
     *
     * Invoked at the end of collision check / broadcast state
     * to indicate tasks related to collision checks are done.
     *
     * Payload includes
     *
     * */
    public synchronized void OnCollisionCheckDone(String agent_name, String[] agent_ids)
    {
        if (FLAG_COLLISION_CHECKS.containsKey(agent_name) || FLAG_INACTIVE.containsKey(agent_name))
        {
            return;
        }

        FLAG_COLLISION_CHECKS.put(agent_name, "");
        if (agent_ids.length > 1)
        {
            Log(agent_name + " reported collision : " + Arrays.toString(agent_ids));
            negotiation_overseer.RegisterCollisionNotification(agent_ids);
        }
    }

    public synchronized String[] GetNegotiations(String agent_name)
    {
        return negotiation_overseer.GetNegotiations(agent_name);
    }

    public synchronized void JoinNegotiationSession(String session_id, AgentHandler agent)
    {
        String msg = negotiation_overseer.AgentJoinSession(session_id, agent);
        Log(msg);
    }

    /**
     * @function Negotiate
     *
     * Invoked at the end of Negotiation sessions to indicate
     * task related to negotiations are done.
     * */
    public synchronized void Negotiated(String agent_name, String session_id)
    {
        FLAG_NEGOTIATIONS_DONE.put(agent_name, "");
        if (!session_id.isEmpty())
        {
            negotiation_overseer.AgentLeaveSession(agent_name, session_id);
        }
    }

    /**
     * Queue AgentHandler
     *
     * @param agent - Agent Handler
     * */
    public void Move(AgentHandler agent, DATA_REQUEST_PAYLOAD_WORLD_MOVE payload)
    {
        // queue agent for movement
        movement_handler.put(agent.getAgentName(), agent, payload);
    }

    public synchronized void Leave(AgentHandler agent)
    {
        FLAG_COLLISION_CHECKS.remove(agent.getAgentName());
        FLAG_INACTIVE.put(agent.getAgentName(), "");

        active_agent_c--;
    }

    public synchronized void Log(String str)
    {
        state_log.add(new Object[]{str, new java.sql.Timestamp(System.currentTimeMillis())});
    }

    public synchronized void LogNegotiation(String key, String value)
    {
        String[] key_data = key.split("-", 2);

        ArrayList<String> data = log_payload.negotiation_logs.getOrDefault(key_data[0], new ArrayList<>());
        data.add(value);

        if (key_data.length == 2)
        {   // clear
            log_payload.negotiation_logs.remove(key_data[0]);
            log_payload.negotiation_logs.put(key, data);

            return;
        }

        log_payload.negotiation_logs.put(key_data[0], data);
    }

    public HashMap<String, Point[]> GetAllBroadcasts()
    {
        HashMap<String, Point[]> broadcasts = new HashMap<>();

        for (String agent_name : clients.keySet())
        {
            broadcasts.put(agent_name, clients.get(agent_name).GetBroadcast());
        }

        return broadcasts;
    }

    public String[] GetBroadcast(String agent_name)
    {
        if (agent_name == null) return new String[0];

        return broadcasts.getOrDefault(agent_name, new String[0]);
    }
}
