package edu.ozu.mapp.system;

import edu.ozu.mapp.agent.Agent;
import edu.ozu.mapp.agent.client.AgentClient;
import edu.ozu.mapp.agent.client.AgentHandler;
import edu.ozu.mapp.agent.client.helpers.FileLogger;
import edu.ozu.mapp.agent.client.models.Contract;
import edu.ozu.mapp.dataTypes.WorldSnapshot;
import edu.ozu.mapp.system.fov.FoV;
import edu.ozu.mapp.system.fov.FoVHandler;
import edu.ozu.mapp.utils.Globals;
import edu.ozu.mapp.utils.JSONWorldWatch;
import edu.ozu.mapp.utils.Point;
import edu.ozu.mapp.utils.PseudoLock;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class WorldOverseer
{
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(WorldOverseer.class);
    private FileLogger file_logger;

    protected   String              WorldID;
    protected   int                 width;
    protected   int                 height;
    protected   int                 active_agent_c;
    protected   Globals.WorldState  curr_state;
    protected   Globals.WorldState  prev_state;

    private boolean IsLooping;
    private boolean join_update_hook_run_once;

    private ScheduledExecutorService service;

    private ConcurrentHashMap<String, AgentClient>  clients;
    /**
     * K: Agent name
     * V: Agent's broadcasted path
     * */
    public ConcurrentHashMap<String, String[]> broadcasts;
    public ConcurrentHashMap<String, Integer>  bank_data;
    public ConcurrentHashMap<String, String>   agent_to_point;
    public ConcurrentHashMap<String, String>   point_to_agent;
    public ConcurrentSkipListSet<String>       active_agents;
    public ConcurrentHashMap<String, String[]> passive_agents;

    protected MovementHandler     movement_handler;
    public NegotiationOverseer negotiation_overseer;

    DATA_LOG_DISPLAY log_payload;

    private long SIM_LOOP_START_TIME;
    private long SIM_LOOP_FINISH_TIME;
    private long SIM_LOOP_DURATION;
    protected int TIME;

    protected ConcurrentHashMap<String, String> FLAG_JOINS;
    protected ConcurrentHashMap<String, String> FLAG_COLLISION_CHECKS;
    protected ConcurrentHashMap<String, String> FLAG_NEGOTIATION_REGISTERED;
    protected ConcurrentHashMap<String, String> FLAG_NEGOTIATIONS_DONE;
    protected ConcurrentHashMap<String, String> FLAG_NEGOTIATIONS_VERIFIED;
    protected ConcurrentHashMap<String, String> FLAG_INACTIVE;

    private Consumer<DATA_LOG_DISPLAY>  UI_LogDrawCallback = (value) -> {};
    private Consumer<String>            UI_StateChangeCallback = (value) -> {};
    private Runnable                    UI_CanvasUpdateHook = () -> {};
    private Runnable                    UI_LoopStoppedHook = () -> {};
    private BiConsumer<String, WorldSnapshot> SNAPSHOT_HOOK = (val1, val2) -> {};
    private ScheduledFuture<?> main_sim_loop;
    private ScheduledFuture<?> overseer_validation_job;

    private Consumer<WorldState> SCENARIO_MANAGER_HOOK_JOIN_UPDATE = (val) -> {};
    private Runnable SCENARIO_MANAGER_HOOK_SIMULATION_FINISHED = () -> {};

    private LeaveActionHandler leaveActionHandler;
    private MoveActionHandler moveActionHandler;
    private FoVHandler foVHandler;

    public WorldOverseer()
    {
        construct();
    }

    private void construct()
    {
        WorldID = "";
        width   = 0;
        height  = 0;
        active_agent_c = 0;

        IsLooping = false;
        join_update_hook_run_once = false;

        clients             = new ConcurrentHashMap<>();
        curr_state          = Globals.WorldState.JOIN;
        prev_state          = Globals.WorldState.NONE;
        movement_handler    = new MovementHandler();
        negotiation_overseer = new NegotiationOverseer();
        negotiation_overseer.bank_update_hook = this::UpdateBankInfo;
        negotiation_overseer.world_log_callback = this::Log;
        negotiation_overseer.log_payload_hook = this::LogNegotiation;

        broadcasts     = new ConcurrentHashMap<>();
        bank_data      = new ConcurrentHashMap<>();
        agent_to_point = new ConcurrentHashMap<>();
        point_to_agent = new ConcurrentHashMap<>();
        active_agents  = new ConcurrentSkipListSet<>();
        passive_agents = new ConcurrentHashMap<>();

        log_payload    = new DATA_LOG_DISPLAY();

        FLAG_JOINS            = new ConcurrentHashMap<>();
        FLAG_COLLISION_CHECKS = new ConcurrentHashMap<>();
        FLAG_NEGOTIATION_REGISTERED = new ConcurrentHashMap<>();
        FLAG_NEGOTIATIONS_DONE = new ConcurrentHashMap<>();
        FLAG_NEGOTIATIONS_VERIFIED = new ConcurrentHashMap<>();
        FLAG_INACTIVE         = new ConcurrentHashMap<>();

        leaveActionHandler = new LeaveActionHandler(this);
        moveActionHandler = new MoveActionHandler(this);
        foVHandler = new FoVHandler(this);

        agents_verify_lock = new PseudoLock();
        overseer_validator_invoke_lock = new PseudoLock();

        TIME = 0;

        System.out.println(string());
    }

    public String string() {
        return this.getClass().getSimpleName() + "@" + Integer.toHexString(System.identityHashCode(this)) +
                "\n clients: " + clients +
                "\n map: " + point_to_agent +
                "\n main_sim_loop: " + (main_sim_loop == null ? "null" : String.valueOf(main_sim_loop.isDone()));
    }

    public void Create(String world_id, int width, int height)
    {
        WorldID     = world_id;
        this.width  = width;
        this.height = height;

        file_logger = FileLogger.getInstance();//.CreateWorldLogger(world_id);
        movement_handler.SetWorldReference(this);

        file_logger.WorldLogCreate(world_id, width, height);
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
        service = Executors.newScheduledThreadPool(clients.size() + 2);
        System.out.println(" > init service: " + service);

        main_sim_loop = service.scheduleAtFixedRate(this::run_loop_container, 0, 250, TimeUnit.MILLISECONDS);
        overseer_validation_job = service.scheduleAtFixedRate(this::overseer_validator, 0, 1, TimeUnit.SECONDS);

        for (String agent_name : clients.keySet())
        {
            AgentClient client = clients.get(agent_name);
            service.scheduleAtFixedRate(update_state_func_generator(client), 100, 500, TimeUnit.MILLISECONDS);
        }
        System.out.println(" > init service: " + service);
    }

    private void run_loop_container()
    {
        try { run(); }
        catch (Exception e) { e.printStackTrace(); }
    }

    @NotNull
    private Runnable update_state_func_generator(AgentClient client)
    {
        return () -> {
            try { client.UpdateState(get_current_state(client.GetAgentName())); }
            catch (Exception e) { e.printStackTrace(); }
        };
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
        data.put("Field of View", String.valueOf(Globals.FIELD_OF_VIEW_SIZE));

        log_payload.world_data = data;

        UI_CanvasUpdateHook.run();

        if (curr_state == prev_state)
        {   // HANDLE ONCE
            return;
        }


        UI_StateChangeCallback.accept(curr_state.toString());

        if (active_agent_c == 0 && IsLooping)
        {   // there are no active agents left!
            IsLooping = false;
            main_sim_loop.cancel(false);
            overseer_validation_job.cancel(true);
            SIM_LOOP_FINISH_TIME = System.nanoTime();

            if (UI_LoopStoppedHook != null) { UI_LoopStoppedHook.run(); }

            SIM_LOOP_DURATION = SIM_LOOP_FINISH_TIME - SIM_LOOP_START_TIME;

            logger.debug("SIM_LOOP_FINISH_TIME=" + SIM_LOOP_FINISH_TIME);
            logger.debug("SIM_LOOP_DURATION   =" + (SIM_LOOP_DURATION / 1E9));

            long _t = System.currentTimeMillis();
            Log("- SIM_LOOP_FINISH_TIME", new java.sql.Timestamp(_t));
            Log(String.format("- SIM_LOOP_DURATION %s seconds", (SIM_LOOP_DURATION / 1E9)), new java.sql.Timestamp(_t));
            file_logger.WorldLogDone(WorldID, _t, (SIM_LOOP_DURATION / 1E9));

            ui_log_draw_callback_invoker(log_payload);
            if (SCENARIO_MANAGER_HOOK_SIMULATION_FINISHED != null)
                SCENARIO_MANAGER_HOOK_SIMULATION_FINISHED.run();

            return;
        }

        switch (curr_state)
        {
            case JOIN:
                UI_CanvasUpdateHook.run();

                if (SCENARIO_MANAGER_HOOK_JOIN_UPDATE != null && !join_update_hook_run_once)
                {   // what the hell is this...
                    SCENARIO_MANAGER_HOOK_JOIN_UPDATE.accept(WorldState.JOINING);

                    if (FLAG_JOINS.size() == active_agent_c) {
                        join_update_hook_run_once = true;
                    }
                }

                if (FLAG_JOINS.size() == active_agent_c)
                {   // ALL REGISTERED AGENTS ARE DONE JOINING
                    Log("- END OF JOIN STATE", logger::info);
                    SNAPSHOT_HOOK.accept(
                        String.format("Initial State - %-23s", new java.sql.Timestamp(System.currentTimeMillis())),
                        generate_snapshot()
                    );
                    SNAPSHOT_HOOK.accept(
                        String.format("MOVE T:%s - %-23s", TIME, new java.sql.Timestamp(System.currentTimeMillis())),
                        generate_snapshot()
                    );
                    prev_state = curr_state;

                    if (SCENARIO_MANAGER_HOOK_JOIN_UPDATE != null)
                        SCENARIO_MANAGER_HOOK_JOIN_UPDATE.accept(WorldState.JOINED);

                    if (IsLooping) { Step(); }
                }

                break;
            case BROADCAST:
                if (FLAG_COLLISION_CHECKS.size() == active_agent_c)
                {   // BROADCASTS DONE
                    Log("- BROADCASTS ARE DONE", logger::info);

                    prev_state = curr_state;

                    if (IsLooping) { Step(); }
                }

                break;
            case NEGOTIATE:
                if (FLAG_NEGOTIATIONS_VERIFIED.size() == active_agent_c)
                {
                    Log("- NEGOTIATIONS ARE VERIFIED " + FLAG_NEGOTIATIONS_VERIFIED.size() + " ?= " + active_agent_c , logger::info);

                    FLAG_NEGOTIATIONS_VERIFIED.clear();

                    prev_state = curr_state;
                    agents_verify_lock.unlock();    // ensure unlock

                    if (IsLooping) { Step(); }
                    break;
                }
                if (FLAG_NEGOTIATIONS_DONE.size() == active_agent_c)
                {   // NEGOTIATIONS ARE COMPLETE
                    Log("- NEGOTIATIONS ARE COMPLETE", logger::info);

                    FLAG_NEGOTIATION_REGISTERED.clear();
                    FLAG_COLLISION_CHECKS.clear();
                    FLAG_NEGOTIATIONS_DONE.clear();

                    /* VERIFY NEGOTIATIONS
                     *
                     * Request verification from all agents.
                     * If one verification fails
                     *  - remove failing from negotiations done
                     *  - remove failing from verified (should not be in in the first place)
                     *  - failing agents should update their state flags to int:1
                     *     `collision_check` function already takes care of this
                     * */
                    agents_verify_paths_after_negotiation();
                }

                break;
            case MOVE:
                if (active_agent_c == movement_handler.size())
                {
                    // flush previous state checks
                    FLAG_NEGOTIATION_REGISTERED.clear();
                    FLAG_NEGOTIATIONS_DONE.clear();
                    FLAG_NEGOTIATIONS_VERIFIED.clear();

                    movement_handler.ProcessQueue(() -> CompletableFuture
                            .runAsync(() -> {
                                Log("- MOVES ARE COMPLETE", logger::info);

                                prev_state = curr_state;

                                // Reset data
                                FLAG_COLLISION_CHECKS.clear();
                                FLAG_NEGOTIATIONS_DONE.clear();

                                TIME++;
                                log_payload.LogAgentLocations(agent_to_point);
                                log_payload.LogWorldTime(TIME);
                                log_payload.LogAgentBankInfo(bank_data);

                                if (IsLooping) {
                                    Step();
                                }
                            })
                            .whenComplete((entity, ex) -> {
                                if (ex != null) {
                                    ex.printStackTrace();
                                    SystemExit.exit(500);
                                }

                                SNAPSHOT_HOOK.accept(
                                        String.format("MOVE T:%s - %-23s", TIME, new java.sql.Timestamp(System.currentTimeMillis())),
                                        generate_snapshot()
                                );
                                ui_log_draw_callback_invoker(log_payload);
                            })
                            .join()
                    );
                }

                break;
            default:
        }

        ui_log_draw_callback_invoker(log_payload);
    }

    private void ui_log_draw_callback_invoker(DATA_LOG_DISPLAY log_payload)
    {
        try {
            UI_LogDrawCallback.accept(log_payload.clone());
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
            SystemExit.exit(500);
        }
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

    public FoV GetFoV(String agent_name)
    {
        // it will be null if agent hasn't joined yet
        if (agent_to_point.get(agent_name) == null) {
            return new FoV();
        }

        return GetAgentFoV(agent_name);
    }

    private FoV GetAgentFoV(String agent_name)
    {
        return this.foVHandler.handler(agent_name);
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
                file_logger.WorldLogJoin(WorldID, active_agent_c, TIME);

                // JOIN state, switch to COLLISION_CHECK state
                logger.debug("switching to BROADCAST");
                curr_state = Globals.WorldState.BROADCAST;

                break;
            case 1:
                file_logger.WorldLogBroadcast(WorldID, TIME);

                STALE_NEGOTIATE_STATE_WAIT_COUNTER = 0;

                // COLLISION_CHECK | BROADCAST state, switch to NEGOTIATION state
                logger.debug("switching to NEGOTIATE");
                curr_state = Globals.WorldState.NEGOTIATE;

                file_logger.WorldLogNegotiate(WorldID, "BEGIN", TIME);

                break;
            case 2:
                file_logger.WorldLogNegotiate(WorldID, "DONE", TIME);

                // NEGOTIATION state, switch to MOVE state
                logger.debug("switching to MOVE");
                curr_state = Globals.WorldState.MOVE;

                break;
            case 3:
                file_logger.WorldLogMove(WorldID, TIME, negotiation_overseer.cumulative_negotiation_count);

                // MOVE state, switch to COLLISION_CHECK | BROADCAST state
                logger.debug("switching to BROADCAST");
                curr_state = Globals.WorldState.BROADCAST;
                break;
            default:
        }

        STALE_SIMULATION_PROCESS_COUNTER = 0;
    }

    public void Loop()
    {
        SIM_LOOP_START_TIME = System.nanoTime();

        logger.debug("SIM_LOOP_START_TIME=" + SIM_LOOP_START_TIME);
        Log("- SIM_START");

        IsLooping = true;

        curr_state = Globals.WorldState.JOIN;
        prev_state = Globals.WorldState.NONE;
//        prev_state = Globals.WorldState.JOIN;
//        if (curr_state == prev_state)
//        {   // already run once, proceed to next step immediately
//            Step();
//        }
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
        client.SetVerifyNegotiationsCallback(this::verify_negotiations_callback);
        client.SetInvalidateHook(this::invalidate_hook);
        client.SetMoveCallback(this::Move);
        client.SetLeaveHook(this::Leave);
        client.SetUpdateBroadcastHook(this::update_broadcast_hook);
        client.SetLogHook(this::Log);

        clients.put(client.GetAgentName(), client);

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
        log_payload.LogAgentBankInfo(bank_data);

        return new String[]{WorldID, width+"x"+height};
    }

    /**
     * Invoked at the end of collision check / broadcast state
     * to indicate tasks related to collision checks are done.
     *
     * Payload includes
     *
     * */
    public synchronized String OnCollisionCheckDone(String agent_name, String[] agent_ids)
    {
        String inactive = FLAG_INACTIVE.get(agent_name);
        if (inactive != null) {
            return "";
        }

        Arrays.sort(agent_ids);

        String collisions = FLAG_COLLISION_CHECKS.get(agent_name);
        if (collisions != null && collisions.equals(Arrays.toString(agent_ids))) {
            // Entry for agent_name already exists
            return FLAG_NEGOTIATION_REGISTERED.get(agent_name);
        }

        FLAG_COLLISION_CHECKS.put(agent_name, Arrays.toString(agent_ids));
        if (agent_ids.length > 1)
        {
            // check if ids are already queued for negotiation
            boolean is_bad = false;
            for (String id : agent_ids) {
                is_bad = is_bad || FLAG_NEGOTIATION_REGISTERED.containsKey(id);
            }
            if (is_bad) return FLAG_NEGOTIATION_REGISTERED.getOrDefault(agent_name, "");

            Log(agent_name + " reported collision : " + Arrays.toString(agent_ids));
            String session_hash = negotiation_overseer.RegisterCollisionNotification(agent_ids);

            if (session_hash == null) return "";

            for (String id : agent_ids)
            {
                FLAG_COLLISION_CHECKS.put(id, Arrays.toString(agent_ids));
                FLAG_NEGOTIATIONS_DONE.remove(id);
                FLAG_NEGOTIATION_REGISTERED.put(id, session_hash);
            }
        }

        return FLAG_NEGOTIATION_REGISTERED.get(agent_name);
    }

    public synchronized String[] GetNegotiations(String agent_name)
    {
        return negotiation_overseer.GetNegotiations(agent_name);
    }

    public synchronized void JoinNegotiationSession(String session_id, AgentHandler agent)
    {
        negotiation_overseer.AgentJoinSession(session_id, agent);
    }

    /**
     * Invoked at the end of Negotiation sessions to indicate
     * task related to negotiations are done.
     * */
    public synchronized void Negotiated(String agent_name, String session_id)
    {
        FLAG_NEGOTIATIONS_DONE.put(agent_name, "");
        broadcasts.put(agent_name, clients.get(agent_name).GetBroadcastSTR());
        if (!session_id.isEmpty())
        {
            negotiation_overseer.AgentLeaveSession(agent_name, session_id);
        }
    }

    private synchronized void UpdateBankInfo(String agent_name, int new_balance)
    {
        if (bank_data.containsKey(agent_name)) {
            bank_data.put(agent_name, new_balance);
        }
    }

    private void __agent_verify_negotiations_logic() {
        for (String agent_id : clients.keySet()) {
            if (passive_agents.containsKey(agent_id)) {
                continue;
            }

            try {
                logger.debug(agent_id + " pre-::verify-negotiations");

                clients.get(agent_id).VerifyNegotiations();

                logger.debug(agent_id + " post::verify-negotiations | " + FLAG_NEGOTIATIONS_DONE.size() + " == " + active_agent_c);
            } catch (Exception exception) {
                logger.error(String.format("ERROR %s%n", exception.getMessage()));
                SystemExit.exit(SystemExit.Status.ERROR_VERIFY_NEGOTIATIONS);
            }
        }
    }

    private PseudoLock agents_verify_lock;
    private synchronized void agents_verify_paths_after_negotiation()
    {
        if (!agents_verify_lock.tryLock()) {
            return;
        }
        logger.debug("acquired agents_verify_lock");

        STALE_NEGOTIATE_STATE_WAIT_COUNTER = 0;
        STALE_SIMULATION_PROCESS_COUNTER = 0;

        logger.debug("verifying agent paths after negotiations");
        __agent_verify_negotiations_logic();

        agents_verify_lock.unlock();
        logger.debug("unlock agents_verify_lock " + FLAG_NEGOTIATIONS_DONE.size() + " == " + active_agent_c);
    }

    private synchronized void verify_negotiations_callback(String agent_id, boolean is_ok)
    {
        if (is_ok) {
            FLAG_NEGOTIATIONS_VERIFIED.put(agent_id, "");
        } else {
            // agent cannot verify paths, there is conflict
            // agent will need to re-do negotiation step
            FLAG_NEGOTIATIONS_DONE.remove(agent_id);
            FLAG_NEGOTIATIONS_VERIFIED.remove(agent_id);
        }
    }

    /**
     * Queue AgentHandler
     *
     * @param agent - Agent Handler
     * */
    public void Move(AgentHandler agent, DATA_REQUEST_PAYLOAD_WORLD_MOVE payload)
    {
        moveActionHandler.handle(agent, payload);
    }

    public synchronized void Leave(AgentHandler agent)
    {
        this.leaveActionHandler.handle(agent);
    }

    private synchronized void update_broadcast_hook(String agent_name, String[] broadcast)
    {
        broadcasts.put(agent_name, broadcast);
    }

    private synchronized void invalidate_hook(String agent_name)
    {
        if (curr_state == Globals.WorldState.NEGOTIATE)
        {
            logger.debug(agent_name + " | invalidating...");
            // check if there is a negotiation to agent's name
            String[] sessions = negotiation_overseer.GetNegotiations(agent_name);
            logger.debug(agent_name + " | invalidating for sessions: " + Arrays.toString(sessions));
            if (sessions.length > 0)
            {   // there is a negotiation
                // decouple negotiations registrations related to agent(s)
                // there will be (should be) only one instance
                String[] agents = negotiation_overseer.InvalidateSession(sessions[0]);
                // need to do 2 passes synchronously to prevent unwanted effects
                for (String identifier : agents) {
                    FLAG_NEGOTIATION_REGISTERED.remove(identifier);
                }
                // invoke validate on all participant agents again
                for (String identifier : agents) {
                    logger.debug(identifier + " | re-verify negotiations");
                    clients.get(identifier).VerifyNegotiations();
                }
            }
            else
            {   // ensure invoke validate [Verify Negotiations]  on `agent_name`
                clients.get(agent_name).VerifyNegotiations();
            }
        }
    }

    public synchronized void Log(String str)
    {
        Log(str, logger::debug);
    }

    public synchronized void Log(String str, Consumer<String> logger)
    {
        logger.accept(str);
        Log(str, new java.sql.Timestamp(System.currentTimeMillis()));
    }

    public synchronized void Log(String str, java.sql.Timestamp timestamp)
    {
        if (log_payload == null) return;
        if (log_payload.world_log == null) return;
        if (!log_payload.world_log.isEmpty() && log_payload.world_log.getLast()[0].equals(str)) return;

        log_payload.world_log.add(new Object[]{str, timestamp});
    }

    public synchronized void LogNegotiation(String key, String value)
    {
        String[] key_data = key.split("-", 2);

        ArrayList<String> data = log_payload.negotiation_logs.getOrDefault(key_data[0], new ArrayList<>());
        data.add(value);

        if (key_data.length == 2)
        {   /* Run when session terminates.
             *
             * Since it is possible for two agents to re-engage in negotiation,
             *  their session hash will be the same.
             *  In such case, the log data will be overwritten.
             *  To prevent this from happening, remove the previous key, and
             *  instead create a new key with the updated data.
             *  Key for the negotiation session instance will be timestamped
             *  separated by `-` in this case. Directly use the key.
             *
             * As this is the case only run when agents end their negotiation session
             *  take a snapshot of the world at this given time.
             * */
            log_payload.negotiation_logs.remove(key_data[0]);
            log_payload.negotiation_logs.put(key, data);

            SNAPSHOT_HOOK.accept(
                String.format("NEGO SESS %s DONE - %-23s", key_data[0].substring(0, 7), key_data[1]),
                generate_snapshot()
            );

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

    public String[] GetAgentData(String agent_id)
    {
        String[] data = new String[]{"", "", ""};

        data[0] = agent_to_point.getOrDefault(agent_id, "");
        data[1] = String.valueOf(bank_data.get(agent_id));
        data[2] = String.valueOf(clients.get(agent_id).GetAgentRemainingPathLength());

        return data;
    }

    public Contract GetMyContract(Agent agent)
    {
        return negotiation_overseer.GetMyContract(agent);
    }

    public String[][] GetLocationConstraints() {
        return passive_agents.values().toArray(new String[0][0]);
    }

    public void SetSnapshotHook(BiConsumer<String, WorldSnapshot> hook)
    {
        SNAPSHOT_HOOK = hook;
    }

    private WorldSnapshot generate_snapshot()
    {
        WorldSnapshot snapshot = new WorldSnapshot();
        snapshot.world_height = height;
        snapshot.world_width  = width;

        for (String client_key : clients.keySet())
        {
            snapshot.agent_keys.add(client_key);
            snapshot.locations.put(client_key, clients.get(client_key).GetCurrentLocation());
            snapshot.paths.put(client_key, clients.get(client_key).GetAgentPlannedPath());
        }

        return snapshot;
    }

    public void SCENARIO_MANAGER_HOOK_JOIN_UPDATE(Consumer<WorldState> hook)
    {
        SCENARIO_MANAGER_HOOK_JOIN_UPDATE = hook;
    }

    public void SCENARIO_MANAGER_HOOK_SIMULATION_FINISHED(Runnable runnable)
    {
        SCENARIO_MANAGER_HOOK_SIMULATION_FINISHED = runnable;
    }

    private int STALE_NEGOTIATE_STATE_WAIT_COUNTER = 0;
    private int STALE_SIMULATION_PROCESS_COUNTER = 0;
    private PseudoLock overseer_validator_invoke_lock;
    private void overseer_validator()
    {
        if (!overseer_validator_invoke_lock.tryLock()) return;

        int client_c = clients.size();
        int inactive_client_c = 0;

        // Get status of all agents
        for (String agent_id : clients.keySet())
        {
            AgentClient client = clients.get(agent_id);

            if (client.IsActive())
            {   // client is active
                FLAG_INACTIVE.remove(agent_id); // you dont belong in here
            }
            else
            {   // client is inactive
                // is it marked like that as well?
                if (FLAG_INACTIVE.containsKey(agent_id))
                {   // good
                    // let's remove you from other flags
                    FLAG_COLLISION_CHECKS.remove(agent_id);
                    FLAG_NEGOTIATION_REGISTERED.remove(agent_id);
                    FLAG_NEGOTIATIONS_DONE.remove(agent_id);
                    FLAG_NEGOTIATIONS_VERIFIED.remove(agent_id);
                }
                else
                {   // why arent you marked inactive
                    FLAG_INACTIVE.put(agent_id, "");
                }

                inactive_client_c += 1;
            }
        }

        int expected_active_agent_c = client_c - inactive_client_c;
        if (active_agent_c != expected_active_agent_c)
        {   // this should not be possible
            logger.warn("UPDATING ACTIVE_AGENT_C " + active_agent_c + " -> " + expected_active_agent_c);
            active_agent_c = expected_active_agent_c;
        }

        switch (curr_state)
        {
            case JOIN:
                break;
            case NEGOTIATE:
                if (STALE_NEGOTIATE_STATE_WAIT_COUNTER >= Globals.STALE_NEGOTIATE_STATE_WAIT_COUNTER_LIMIT)
                {
                    logger.warn("THREAD HAS BEEN STALE FOR : " + STALE_NEGOTIATE_STATE_WAIT_COUNTER + " SECONDS");
                    STALE_NEGOTIATE_STATE_WAIT_COUNTER = 0;

                    logger.warn("ACTIVE NEGOTIATION COUNT  : " + negotiation_overseer.ActiveCount());
                    if (negotiation_overseer.ActiveCount() != 0)
                    {
                        logger.warn("> THERE ARE STILL ACTIVE NEGOTIATIONS");

                        // if there are active sessions...
                        // check if their agents have joined
                        negotiation_overseer.ActiveSessions().values().forEach(session ->
                        {
                            // has session started??
                            // log negotiation session
                            logger.warn(
                                    "> SESSION " + session.GetSessionID() +
                                    " FOR " + Arrays.toString(session.GetAgentIDs()) +
                                    " :: " + session.GetState()
                            );
                            if (session.IsJoining()) {
                                // agents haven't joined for a while
                                for (String agent_id : session.GetAgentIDs()) {
                                    clients.get(agent_id).VerifyNegotiations();
                                }
                            }
                        });

                        STALE_NEGOTIATE_STATE_WAIT_COUNTER = 0;
                        break;
                    }

                    logger.warn("FORCE REVALIDATE ACTIVE NEGOTIATE STATE --");
                    logger.warn("INVALIDATE FLAG_NEGOTIATIONS_DONE");
                    FLAG_NEGOTIATIONS_DONE.clear();

                    logger.warn("INVOKE client::VerifyNegotiations");
                    __agent_verify_negotiations_logic();
                }
                else
                {
                    STALE_NEGOTIATE_STATE_WAIT_COUNTER += 1;
                }
                break;
            case BROADCAST:
            case MOVE:
            default:
                if (STALE_SIMULATION_PROCESS_COUNTER < Globals.STALE_SIMULATION_PROCESS_COUNTER_LIMIT)
                {
                    STALE_SIMULATION_PROCESS_COUNTER += 1;
//                    logger.warn("> TIME OUT TICK " + STALE_SIMULATION_PROCESS_COUNTER);
                    overseer_validator_invoke_lock.unlock();
                }
                else
                {
                    STALE_SIMULATION_PROCESS_COUNTER = 0;
                    logger.warn("TIME OUT");
                    overseer_validator_invoke_lock.unlock();
                    SystemExit.exit(SystemExit.Status.TIMEOUT);
                }
        }

        overseer_validator_invoke_lock.unlock();
    }

    public void close()
    {   // safe close
        service.shutdown();
    }

    public void kill()
    {   // terminate all threads
        if (main_sim_loop != null) {
            main_sim_loop.cancel(false);
        }
        if (overseer_validation_job != null) {
            overseer_validation_job.cancel(true);
        }
        if (service != null) {
            service.shutdownNow();
        }
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getTime() {
        return this.TIME;
    }

    public String getWorldID() {
        return this.WorldID;
    }
}
