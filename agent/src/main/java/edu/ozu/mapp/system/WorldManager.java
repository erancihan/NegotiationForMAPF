package edu.ozu.mapp.system;

import edu.ozu.mapp.agent.client.AgentClient;
import edu.ozu.mapp.agent.client.AgentHandler;
import edu.ozu.mapp.agent.client.helpers.FileLogger;
import edu.ozu.mapp.utils.Globals;
import edu.ozu.mapp.utils.JSONWorldWatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class WorldManager
{
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(WorldManager.class);
    private FileLogger flogger;

    protected   String              WorldID = "";
    protected   int                 width   = 0;
    protected   int                 height  = 0;
    private     int                 active_agent_c = 0;
    private     Globals.WorldState  curr_state;
    private     Globals.WorldState  prev_state;

    private boolean IsLooping = false;

    ScheduledExecutorService service;

    private ConcurrentHashMap<String, AgentClient>  clients;
    /**
     * K: Agent name
     * V: Agent's broadcasted path
     * */
    private ConcurrentHashMap<String, String[]>     broadcasts;

    private MovementHandler movement_handler;

    private ArrayList<Object[]> state_log = new ArrayList<>();

    private long SIM_LOOP_START_TIME;
    private long SIM_LOOP_FINISH_TIME;
    private long SIM_LOOP_DURATION;

    private HashSet<String> FLAG_JOINS;
    private HashSet<String> FLAG_BROADCASTS;
    private HashSet<String> FLAG_NEGOTIATIONS;

    private BiConsumer<Map<String, String>, ArrayList<Object[]>>    UI_LogDrawCallback;
    private Consumer<String>                                        UI_StateChangeCallback;
    private Runnable                                                UI_CanvasUpdateHook;
    private Runnable                                                UI_LoopStoppedHook;

    public WorldManager()
    {
        clients          = new ConcurrentHashMap<>();
        curr_state       = Globals.WorldState.JOIN;
        prev_state       = Globals.WorldState.NONE;
        movement_handler = MovementHandler.getInstance();

        FLAG_JOINS          = new HashSet<>();
        FLAG_BROADCASTS     = new HashSet<>();
        FLAG_NEGOTIATIONS   = new HashSet<>();
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

    public void SetLogDrawCallback(BiConsumer<Map<String, String>, ArrayList<Object[]>> callback)
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
        // TODO POPULATE DATA

        if (curr_state == prev_state)
        {   // HANDLE ONCE
            return;
        }

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

                    if (IsLooping) { Step(); }
                }

                break;
            case BROADCAST:
                if (FLAG_BROADCASTS.size() == active_agent_c)
                {   // BROADCASTS DONE
                    if (IsLooping) { Step(); }
                }

                break;
            case NEGOTIATE:
                if (FLAG_NEGOTIATIONS.size() == active_agent_c)
                {   // NEGOTIATIONS ARE COMPLETE
                    if (IsLooping) { Step(); }
                }

                break;
            case MOVE:
                if (active_agent_c == movement_handler.size())
                {
                    movement_handler.ProcessQueue();
                }
                if (movement_handler.size() == 0)
                {   // queue is empty
                    // switch to BROADCAST
                    if (IsLooping) Step();
                }

                UI_CanvasUpdateHook.run();
                break;
            default:
        }

        UI_LogDrawCallback.accept(data, state_log);
    }

    private JSONWorldWatch get_current_state(String agent_name)
    {
        // TODO
        return null;
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
                curr_state = Globals.WorldState.BROADCAST;
                break;
            case 1:
                // COLLISION_CHECK | BROADCAST state, switch to NEGOTIATION state
                curr_state = Globals.WorldState.NEGOTIATE;
                break;
            case 2:
                // NEGOTIATION state, switch to MOVE state
                curr_state = Globals.WorldState.MOVE;
                break;
            default:
        }
    }

    public void Loop()
    {
        SIM_LOOP_START_TIME = System.nanoTime();

        logger.debug("SIM_LOOP_START_TIME=" + SIM_LOOP_START_TIME);
        state_log.add(new Object[]{"- SIM_START", new java.sql.Timestamp(System.currentTimeMillis())});

        IsLooping = true;
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
        client.SetBroadcastCallback(this::Broadcast);
        client.SetNegotiatedCallback(this::Negotiated);
        client.SetMoveCallback(this::Move);

        clients.put(client.GetAgentName(), client);

        logger.info(String.format("Agent %s has Registered", client.GetAgentName()));
        state_log.add(new Object[]{String.format("Agent %s has Registered", client.GetAgentName()), new java.sql.Timestamp(System.currentTimeMillis()) });

        client.WORLD_HANDLER_JOIN_HOOK();

        active_agent_c++;
    }

    public synchronized String[] Join(DATA_REQUEST_PAYLOAD_WORLD_JOIN payload)
    {
        FLAG_JOINS.add(payload.AGENT_NAME);

        // TODO set map data

        // TODO set broadcast data

        // TODO set bank data, TOKEN for agent

        // TODO register agent as ACTIVE

        return new String[]{WorldID, width+"x"+height};
    }

    /**
     * @function Broadcast
     *
     * Invoked at the end of broadcasting process to indicate
     * task related to broadcasting are done.
     * */
    public synchronized void Broadcast(String agent_name, String[] broadcast)
    {
        if (FLAG_BROADCASTS.contains(agent_name))
        {
            return;
        }

        FLAG_BROADCASTS.add(agent_name);
        broadcasts.put(agent_name, broadcast);
    }

    /**
     * @function Negotiate
     *
     * Invoked at the end of Negotiation sessions to indicate
     * task related to negotiations are done.
     * */
    public synchronized void Negotiated(String agent_name)
    {
        FLAG_NEGOTIATIONS.add(agent_name);
    }

    /**
     * Queue AgentHandler
     *
     * @param agent - Agent Handler
     * */
    public void Move(AgentHandler agent, HashMap<String, Object> payload)
    {
        // queue agent for movement
        movement_handler.put(agent.getAgentName(), agent);
    }

    public void Log(String str)
    {
        state_log.add(new Object[]{str, new java.sql.Timestamp(System.currentTimeMillis())});

        UI_LogDrawCallback.accept(new HashMap<>(), state_log);
    }
}
