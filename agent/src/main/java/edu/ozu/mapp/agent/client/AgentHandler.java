package edu.ozu.mapp.agent.client;

import com.google.gson.Gson;
import edu.ozu.mapp.agent.Agent;
import edu.ozu.mapp.agent.client.helpers.ConflictCheck;
import edu.ozu.mapp.agent.client.helpers.ConflictInfo;
import edu.ozu.mapp.agent.client.helpers.FileLogger;
import edu.ozu.mapp.agent.client.models.Contract;
import edu.ozu.mapp.dataTypes.Constraint;
import edu.ozu.mapp.dataTypes.CollisionCheckResult;
import edu.ozu.mapp.system.DATA_REQUEST_PAYLOAD_WORLD_JOIN;
import edu.ozu.mapp.system.DATA_REQUEST_PAYLOAD_WORLD_MOVE;
import edu.ozu.mapp.system.SystemExit;
import edu.ozu.mapp.system.WorldOverseer;
import edu.ozu.mapp.utils.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.util.Assert;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class AgentHandler {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(AgentHandler.class);
    private FileLogger file_logger;

    private String AGENT_NAME;
    private String WORLD_ID = "";
    private Agent agent;

    private Gson gson;

    private int is_moving = 1;
    private String conflict_location = "";

    private Function<DATA_REQUEST_PAYLOAD_WORLD_JOIN, String[]> WORLD_HANDLER_JOIN;
    private BiFunction<String, String[], String>                WORLD_HANDLER_COLLISION_CHECK_DONE;
    private Function<String, Contract>                          WORLD_OVERSEER_HOOK_GET_NEGOTIATION_CONTRACT;
    private Function<String, String[]>                          WORLD_HANDLER_GET_NEGOTIATION_SESSIONS;
    private BiConsumer<String, AgentHandler>                    WORLD_OVERSEER_JOIN_NEGOTIATION_SESSION;
    private BiConsumer<String, String>                          WORLD_OVERSEER_NEGOTIATED;
    private BiConsumer<String, Boolean>                         WORLD_OVERSEER_CALLBACK_VERIFY_NEGOTIATIONS;
    private BiConsumer<AgentHandler, DATA_REQUEST_PAYLOAD_WORLD_MOVE> WORLD_OVERSEER_MOVE;
    private Consumer<AgentHandler>                              WORLD_OVERSEER_HOOK_LEAVE;
    private BiConsumer<String, String[]>                        WORLD_OVERSEER_HOOK_UPDATE_BROADCAST;
    private Consumer<String>                                    WORLD_OVERSEER_HOOK_LOG;
    private Consumer<String>                                    WORLD_OVERSEER_HOOK_INVALIDATE;

    private final PseudoLock handle_state_run_once_at_a_time_lock;
    private final PseudoLock agent_handler_verify_negotiations_lock;

    private String[][] fov;

    AgentHandler(Agent client) {
        Assert.notNull(client.START, "«START cannot be null»");
        Assert.notNull(client.AGENT_ID, "«AGENT_ID cannot be null»");

        agent = client;
        agent.SetHandlerRef(this);

        // init file logger
        file_logger = FileLogger.getInstance();

        AGENT_NAME = client.AGENT_NAME;

        gson = new Gson();

        handle_state_run_once_at_a_time_lock = new PseudoLock();
        agent_handler_verify_negotiations_lock = new PseudoLock();
    }

    public String getAgentName()
    {
        return AGENT_NAME;
    }

    public String getID()
    {
        return agent.AGENT_ID;
    }

    public void WORLD_HANDLER_JOIN_HOOK()
    {
        DATA_REQUEST_PAYLOAD_WORLD_JOIN payload = new DATA_REQUEST_PAYLOAD_WORLD_JOIN();
        payload.AGENT_NAME      = agent.AGENT_ID;
        payload.AGENT_ID        = agent.AGENT_ID;
        payload.LOCATION        = agent.START;
        payload.BROADCAST       = agent.GetOwnBroadcastPath();
        payload.INIT_TOKEN_C    = agent.initial_tokens;

        String[] response = WORLD_HANDLER_JOIN.apply(payload);
        WORLD_ID = response[0];

        logger.info("joining " + WORLD_ID + " | serverless");

        agent.setWORLD_ID(WORLD_ID);
        agent.dimensions = response[1];

//        file_logger.setWorldID(WORLD_ID);            // SET AGENT WORLD INFO ON LOGGER
        file_logger.AgentLogInfo(agent, "JOIN");      // LOG AGENT INFO ON JOIN
//        file_logger.LogAgentInfo(agent, "JOIN");
        file_logger.WorldLogAgentJoin(agent);    // LOG AGENT JOIN

        // at this point, Agent will be Registered to World
        // and once world starts running, Client will start invoking UpdateState function
        // which will call UpdateState function
    }

    // TODO Rename
    public void UpdateState(JSONWorldWatch watch)
    {
        handleState(watch);
    }

    /**
     * The function that is invoked after agent joins a world. Allows agent
     * to observe the state of the environment.
     * <p>
     * Essentially handles invocations of other functions depending on the
     * {world_state}:
     * 0 -> join
     * 1 -> collision check/broadcast
     * 2 -> negotiation step
     * 3 -> move step
     *
     * @param watch: JSONWorldWatch
     */
    private int[] state_flag = new int[2];
    private void handleState(JSONWorldWatch watch) {
        if (!handle_state_run_once_at_a_time_lock.tryLock()) return;

        switch (watch.world_state) {
            case 0: // join
                state_flag = new int[]{0, 0};
                break;
            case 1: // collision check/broadcast
                if (state_flag[0] != 1)
                {
                    fov = watch.fov;

                    collision_check();
                    state_flag[1] = watch.time_tick;
                }
                break;
            case 2: // negotiation state
                if (state_flag[0] != 2)
                {
                    fov = watch.fov;

                    negotiate();
                    state_flag[0] = 2;
                    state_flag[1] = watch.time_tick;
                }
                break;
            case 3: // move and update broadcast
                if (state_flag[0] != 3)
                {   // move once
                    fov = watch.fov;

                    move();
                    state_flag[0] = 3;
                    state_flag[1] = watch.time_tick;
                }
                break;
            default:
                logger.error("«unhandled world state:" + watch.world_state + "»");
                break;
        }

        // there are no async calls in this function
        // it is safe to unlock at the end of the function
        handle_state_run_once_at_a_time_lock.unlock();
    }

    /**
     * Collision Check function.
     *
     * Returns <code>true</code> if collision check result is safe.
     *
     * @return is_ok
     * */
    private CollisionCheckResult collision_check()
    {
        if (is_moving == 0) return new CollisionCheckResult();

        logger.debug(agent.AGENT_ID + " | checking for collisions...");

        CollisionCheckResult result = null;
        try {
            String[][] fov_data = WorldOverseer.getInstance().GetFoV(agent.AGENT_ID);
            result = getCollidingAgents(fov_data);
        } catch (Exception ex) {
            ex.printStackTrace();
            SystemExit.exit(500);
        }
        switch (result.type) {
            case COLLISION:
                // There is a collision in path! Resolve this first
                logger.debug(agent.AGENT_ID + " | notify negotiation > " + Arrays.toString(result.agent_ids));
                /* Collision check step is done update state
                 *  flag to 1 (collision check/broadcast) to
                 *  indicate the current state.
                 * If current state is 1, it will not run again.
                 * If current state is 2, setting state flag to 1
                 *  will make negotiate state run again.
                 *  */
                state_flag[0] = 1;
                WORLD_HANDLER_COLLISION_CHECK_DONE.apply(agent.AGENT_ID, result.agent_ids);

                return result;
            case OBSTACLE:
                // TODO LOG OBSTACLE UPDATE
                // There is an obstacle in the path! Update route according to constraints
                logger.debug(agent.AGENT_ID + " | found obstacle");
                // Re-calculate path starting from here on out
                agent.path = update_agent_path_from_pos_to_dest(new ArrayList<>());
                // I have updated my path, make broadcast
                logger.debug(agent.AGENT_ID + " | updated path, invalidate");
                WORLD_OVERSEER_HOOK_UPDATE_BROADCAST.accept(agent.AGENT_ID, agent.GetOwnBroadcastPath());
                WORLD_OVERSEER_HOOK_INVALIDATE.accept(agent.AGENT_ID);

                return result;
            case NONE:
            default:
                state_flag[0] = 1;
                WORLD_HANDLER_COLLISION_CHECK_DONE.apply(agent.AGENT_ID, new String[]{agent.AGENT_ID});

                return new CollisionCheckResult();
        }
    }

    private CollisionCheckResult getCollidingAgents(String[][] data)
    {
        Arrays.sort(data, Comparator.comparing(a -> a[0]));
        List<String[]> broadcasts = Arrays.stream(data).collect(Collectors.toList());
        Set<String> agent_ids = new HashSet<>();
        String[] own_path = agent.GetOwnBroadcastPath();

        ArrayList<CollisionCheckResult> conflicts = new ArrayList<>();

        agent_ids.add(agent.AGENT_ID); // add own data
        for (String[] broadcast : broadcasts)
        {
            if (broadcast[0].equals(agent.AGENT_ID))
            {   // own data
                continue;
            }

            if (broadcast[2].equals("inf"))
            {   // OBSTACLE IN FoV
                // always register obstacle
                agent.constraints.add(new Constraint(new Point(broadcast[1], "-")));

                int idx = Arrays.asList(own_path).indexOf(broadcast[1]);
                if (idx > 0)
                {   // OBSTACLE IS IN WAY
                    conflicts.add(new CollisionCheckResult(idx, CollisionCheckResult.Type.OBSTACLE));
                }
                continue;
            }

            String[] path = new Path(broadcast[2]).toStringArray();
            ConflictInfo conflict_info = new ConflictCheck().check(own_path, path);
            if (conflict_info.hasConflict)
            {
                agent_ids.add(broadcast[0]);
                String conflict_location = "";
                // TODO
                // since first Vertex Conflict or Swap Conflict found
                // is immediately returned
                // logging the conflict location here should pose no issue
                if (conflict_info.type == ConflictCheck.ConflictType.SwapConflict)
                    conflict_location = own_path[conflict_info.index] + " -> " + own_path[conflict_info.index + 1];
                else
                    conflict_location = own_path[conflict_info.index];

                logger.info(agent.AGENT_ID + " | found " + conflict_info.type + " at " + conflict_location + " | " + Arrays.toString(broadcast));

                CollisionCheckResult ret = new CollisionCheckResult(CollisionCheckResult.Type.COLLISION);
                ret.agent_ids = agent_ids.toArray(new String[0]);
                ret.index = conflict_info.index;
                ret.conflict_location = conflict_location;

                conflicts.add(ret);
            }
        }

        Arrays.sort(conflicts.toArray(new CollisionCheckResult[0]), Comparator.comparing(a -> a.index));
        if (conflicts.size() > 0) {
            conflict_location = conflicts.get(0).conflict_location;
            return conflicts.get(0);
        } else {
            conflict_location = "";
            return new CollisionCheckResult();
        }
    }

    private void negotiate()
    {
        if (is_moving == 0) return;

        String[] sessions = WORLD_HANDLER_GET_NEGOTIATION_SESSIONS.apply(agent.AGENT_ID);
        logger.debug(agent.AGENT_ID + " | negotiate state > " + Arrays.toString(sessions));
        if (sessions.length > 0)
        {
            for (String sid : sessions)
            {
                if (sid.isEmpty()) { continue; }

                agent.SetConflictLocation(sid, conflict_location);

                WORLD_OVERSEER_JOIN_NEGOTIATION_SESSION.accept(sid, this);

                logger.debug(agent.AGENT_ID + " | " + sid);
            }
        } else {
            WORLD_OVERSEER_NEGOTIATED.accept(agent.AGENT_ID, "");
        }
    }

    private void move() {
        // check if agent is moving
        // 0: stopped  | out of moves
        // 1: can move | has moves to move
        if (is_moving == 0) return;

        if (agent.time + 1 < agent.path.size()) {
//            System.out.println("moving " + agent.AGENT_ID);

            // make Agent move
            DATA_REQUEST_PAYLOAD_WORLD_MOVE payload = new DATA_REQUEST_PAYLOAD_WORLD_MOVE();
            payload.AGENT_NAME = agent.AGENT_ID;
            payload.CURRENT_LOCATION = agent.POS;
            payload.NEXT_LOCATION    = new Point(agent.path.get(agent.time + 1), "-");
            payload.BROADCAST        = agent.GetNextBroadcast();

            WORLD_OVERSEER_MOVE.accept(this, payload);
        } else {
//            System.out.println(agent.AGENT_ID + " stopping t:" + agent.time + " path: " + agent.path);
            // no more moves left, agent should stop
            is_moving = 0;

            // let the world know that you are done with it!
            leave();
        }

        file_logger.AgentLogInfo(agent, "MOVE");  // LOG AGENT INFO ON MOVE CALL
    }

    public void DoMove(JSONAgent response)
    {
        // update internal clock
        agent.time = agent.time + 1;

        // get next point
        Point next_point = new Point(agent.path.get(Math.min(agent.time, agent.path.size() - 1)).split("-"));

        // validate next location
        Assert.isTrue(
                (response.agent_x + "-" + response.agent_y).equals(next_point.key),
                "next point and move action does not match! \n" + response.agent_x + "-" + response.agent_y + " != " + next_point.key + "\n PATH:" + agent.path + "\n"
        );

        // update current position
        logger.debug(agent.AGENT_ID + " | moving | " + agent.POS.key + " -> " + next_point.key);
        agent.POS = next_point;

        agent.OnMove(response);
    }

    /**
     * Function to be called when world watch disconnects
     */
    public void leave()
    {
        file_logger.AgentLogInfo(agent, "LEAVE"); // LOG AGENT INFO ON LEAVE
        file_logger.WorldLogAgentLeave(agent);    // LOG AGENT LEAVING

        WORLD_OVERSEER_HOOK_LEAVE.accept(this);

        logger.debug("World@leave{world:"+WORLD_ID+": , agent:"+agent.AGENT_ID+"}");
    }

    // todo handle better later
    public void exit() {
        leave();
        System.exit(0);
    }

    public String getDest() {
        return agent.DEST.key;
    }

    public String getStart() {
        return agent.START.key;
    }

    public String GetAgentID() {
        return agent.AGENT_ID;
    }

    public ArrayList<Point> GetAgentPlannedPath()
    {
        return agent.path.stream().map(p -> new Point(p, "-")).collect(Collectors.toCollection(ArrayList::new));
    }

    public Point[] GetBroadcast()
    {
        return Arrays.stream(agent.GetOwnBroadcastPath()).map(p -> new Point(p, "-")).toArray(Point[]::new);
    }

    public String[] GetBroadcastSTR()
    {
        return agent.GetOwnBroadcastPath();
    }

    public void OnReceiveState(State state)
    {
        // New state received
        // fetch current Contract
        Contract contract = state.contract;

        if (contract != null) agent.history.put(contract);

        agent.onReceiveState(state);
    }

    public void PreNegotiation(String session_id, State state)
    {
        agent.history.setCurrentNegotiationID(session_id);

        // since this is a Bi-lateral negotiation, there should be only
        // two participant in the negotiation
        Assert.isTrue(state.agents.length == 2, "There are more agents than expected");

        // filter matching out
        agent.current_opponent = Arrays.stream(state.agents).filter(a -> !a.equals(agent.AGENT_ID)).collect(Collectors.toList()).get(0);

        file_logger.AgentLogPreNegotiation(this, session_id);
        agent.PreNegotiation(state);
    }

    public final Action OnMakeAction(Contract contract)
    {
        Action action = null;
        try
        {
            logger.debug(agent.AGENT_ID + " | Making Action | " + contract.sess_id);
            action = agent.onMakeAction(contract);
            action.take();
            logger.debug(agent.AGENT_ID + " | Taking Action | " + contract.sess_id + " " + action);

            file_logger.AgentLogNegotiationAction(this, action);
        }
        catch (Exception exception)
        {
            System.err.println(agent.AGENT_ID + " " + agent.POS + " -> " + agent.DEST);
            System.err.println(agent.constraints);
            exception.printStackTrace();
            SystemExit.exit(500);
        }

        return action;
    }

    @SuppressWarnings("Duplicates")
    //<editor-fold defaultstate="collapsed" desc="Accept Last Bids">
    public void AcceptLastBids(Contract contract) {
        List<String> new_path = new ArrayList<>();

        // use contract to apply select paths
        // if 'x' is self, update planned path
        if (contract.x.equals(agent.AGENT_ID)) {
            new_path = handle_win_condition(contract);
        } else {
            // else use 'Ox' & others as constraint & re-calculate path
            new_path = handle_lose_condition(contract);
        }

        // update global path
        logger.debug(String.format("%s | PATH BEFORE %s", agent.AGENT_ID, agent.path));

        WORLD_OVERSEER_HOOK_LOG
            .accept(String.format(
                "PATH UPDATE AGENT: %s \n%23s PATH BEFORE: %s\n%23s PATH AFTER : %s",
                agent.AGENT_ID,
                "", Utils.toString(agent.path),
                "", Utils.toString(new_path)
            ));

        agent.path = new_path;
        logger.debug(String.format("%s | PATH AFTER  %s", agent.AGENT_ID, agent.path));

        WORLD_OVERSEER_HOOK_UPDATE_BROADCAST.accept(agent.AGENT_ID, agent.GetOwnBroadcastPath());
        agent.OnAcceptLastBids(contract);
    }

    @NotNull
    private List<String> handle_win_condition(Contract contract) {
        // WIN condition
        logger.debug(String.format("%s | WON | %s", agent.AGENT_ID, contract.print()));
        logger.debug(String.format("%s | current location : %s", agent.AGENT_ID, agent.POS));

        Point[] Ox = contract.GetOx();

        logger.debug(String.format("%s | ASSERT POS{%s} == Ox[0]{%s}", agent.AGENT_ID, agent.POS.key, Ox[0]));
        Assert.isTrue(agent.POS.equals(Ox[0]), agent.AGENT_ID + "inconsistent bid location for " + agent.POS + " & " + Ox[0]);

        logger.debug(String.format("%s | ACCEPTED PATH %s", agent.AGENT_ID, Arrays.toString(Ox)));

        // acknowledge negotiation result.
        // since i have won, the path that i have chosen for my self is locked

        List<String> path_next = new ArrayList<>();
        // add path up till now, including now
        for (int idx = 0; idx < agent.path.size() && idx <= agent.time; idx++) {
            logger.debug(String.format("%s | PATH UPDATE | ADDING %s %s", agent.AGENT_ID, idx, agent.path.get(idx)));
            path_next.add(agent.path.get(idx));
        }

        assert agent.path.get(agent.time).equals(agent.POS.key);
        assert agent.path.get(agent.path.size()-1).equals(agent.POS.key);
        // double assert path end and agreed path start are linked
        assert path_next.get(path_next.size()-1).equals(Ox[0].key);

        // add Ox
        for (int idx = 1; idx < Ox.length; idx++) {
            path_next.add(Ox[idx].key);
        }

        // calculate path from last to destination
        // i do not have to worry about opponent's path since i won
        Point end = new Point(path_next.get(path_next.size()-1), "-");

        logger.debug(String.format("%s | CALCULATING REST OF THE PATH", agent.AGENT_ID));
        logger.debug(String.format("%s | FROM %s TO %s", agent.AGENT_ID, end, agent.DEST));

        List<String> rest = agent.calculatePath(end, agent.DEST);
        if (rest == null) {
            logger.error(agent.AGENT_ID + " | REST cannot be null!");
            SystemExit.exit(500);
        }

        logger.debug(String.format("%s | CALCULATED PATH", agent.AGENT_ID));
        logger.debug(String.format("%s | %s", agent.AGENT_ID, Arrays.toString(rest.toArray())));

        if (rest.size() > 0) {
            // ensure that connection points match
            Assert.isTrue(path_next.get(path_next.size() - 1).equals(rest.get(0)),"Something went wrong while accepting last bids!");

            // merge...
            for (int idx = 1; idx < rest.size(); idx++) {
                path_next.add(rest.get(idx));
            }
        }
        agent.winC++;

        return path_next;
    }

    @NotNull
    private List<String> handle_lose_condition(Contract contract)
    {   // LOSE condition
        ArrayList<Constraint> constraints = update_agent_constraints(contract);
        logger.debug(String.format("%s | LOST | %s", agent.AGENT_ID, contract.print()));
        agent.loseC++;

        return update_agent_path_from_pos_to_dest(constraints);
    }

    /**
     * Run when agent is lost to set accepted Ox points as constraints
     * */
    private ArrayList<Constraint> update_agent_constraints(Contract contract)
    {
        if (agent.constraints == null)
            agent.constraints = new ArrayList<>();

        // create constraints, add Ox as constraint
        int i = 0;
        for (Point point : contract.GetOx())
        {
            Constraint constraint = new Constraint(point, agent.time + i);
            if (!agent.constraints.contains(constraint))
                agent.constraints.add(constraint);
            logger.debug(String.format("%s | ADDED CONSTRAINT %s:%s", agent.AGENT_ID, point, agent.time + i));
            i++;
        }

        return agent.constraints;
    }
    //</editor-fold>

    @NotNull
    private List<String> update_agent_path_from_pos_to_dest(ArrayList<Constraint> constraints)
    {
        logger.debug(String.format("%s | current location : %s", agent.AGENT_ID, agent.POS));

        List<String> path_next = new ArrayList<>();
        // add path up until now, including now
        for (int idx = 0; idx < agent.path.size() && idx <= agent.time; idx++)
        {
            path_next.add(agent.path.get(idx));
        }

        assert agent.path.get(agent.time).equals(agent.POS.key);
        assert agent.path.get(agent.path.size()-1).equals(agent.POS.key);

        // calculate rest of the path
        List<String> rest = agent.calculatePath(agent.POS, agent.DEST, constraints);
        if (rest == null) {
            logger.error(agent.AGENT_ID + " | REST cannot be null!");
            SystemExit.exit(500);
        }

        // ensure that connection points match
        Assert.isTrue(agent.POS.key.equals(rest.get(0)), "Something went wrong while accepting last bids!");
        Assert.isTrue(agent.DEST.key.equals(rest.get(rest.size() - 1)), "Something went wrong while accepting last bids!");

        // merge...
        // since current POS is already in 'rest'@0, we can just add it
        for (int idx = 1; idx < rest.size(); idx++) {
            path_next.add(rest.get(idx));
        }

        return path_next;
    }

    public void PostNegotiation(Contract contract)
    {
        file_logger.AgentLogPostNegotiation(this, contract.sess_id, contract.x.equals(agent.AGENT_ID));
        agent.PostNegotiation();

        if (WORLD_OVERSEER_NEGOTIATED != null)
        {
            WORLD_OVERSEER_NEGOTIATED.accept(agent.AGENT_ID, contract.sess_id);
        }
    }

    public void VerifyNegotiations()
    {
        if (!agent_handler_verify_negotiations_lock.tryLock()) return;

        // verify path is conflict free at the moment
        CollisionCheckResult result = collision_check();
        boolean is_ok = result.type == CollisionCheckResult.Type.NONE;

        WORLD_OVERSEER_CALLBACK_VERIFY_NEGOTIATIONS.accept(agent.AGENT_ID, is_ok);

        agent_handler_verify_negotiations_lock.unlock();
        logger.debug(agent.AGENT_ID + " | verified negotiation is_ok:" + is_ok);
    }

    public int UpdateTokenCountBy(int i)
    {
        if (agent.current_tokens + i < 0) {
            logger.error(agent.AGENT_ID + " | OH NO!!! AGENT CANNOT HAVE NEGATIVE TOKEN COUNT");
            SystemExit.exit(500);
        }
        agent.current_tokens += i;

        return agent.current_tokens;
    }

    public Agent GetAgent()
    {
        return agent;
    }

    public String[][] GetCurrentFoVData() {
        return fov;
    }

    public int GetRemainingPathLength()
    {
        return (agent.path.size()-1) - agent.time;
    }

    public String GetCurrentLocation()
    {
        return agent.POS.key;
    }

    public boolean IsActive()
    {
        // is agent out of move to make?
        if (agent.time >= (agent.path.size()-1))
        {   // are you marked
            if (is_moving == 1) {
                // why are you still moving?
                is_moving = 0;
                leave();
            }

            return false;
        }

        return true;
    }

    /** ================================================================================================================ **/

    public void SetJoinCallback(Function<DATA_REQUEST_PAYLOAD_WORLD_JOIN, String[]> function)
    {
        WORLD_HANDLER_JOIN = function;
    }

    public void SET_COLLISION_CHECK_DONE(BiFunction<String, String[], String> function)
    {
        WORLD_HANDLER_COLLISION_CHECK_DONE = function;
    }

    public void GET_NEGOTIATION_CONTRACT(Function<String, Contract> function)
    {
        WORLD_OVERSEER_HOOK_GET_NEGOTIATION_CONTRACT = function;
    }

    public void SetGetNegotiationsHook(Function<String, String[]> function)
    {
        WORLD_HANDLER_GET_NEGOTIATION_SESSIONS = function;
    }

    public void SET_NEGOTIATION_JOIN_SESSION(BiConsumer<String, AgentHandler> function)
    {
        WORLD_OVERSEER_JOIN_NEGOTIATION_SESSION = function;
    }

    public void SetNegotiatedCallback(BiConsumer<String, String> function)
    {
        WORLD_OVERSEER_NEGOTIATED = function;
    }

    public void SetMoveCallback(BiConsumer<AgentHandler, DATA_REQUEST_PAYLOAD_WORLD_MOVE> function)
    {
        WORLD_OVERSEER_MOVE = function;
    }

    public void SET_WORLD_OVERSEER_HOOK_LEAVE(Consumer<AgentHandler> leave)
    {
        WORLD_OVERSEER_HOOK_LEAVE = leave;
    }

    public void SET_WORLD_OVERSEER_HOOK_LOG(Consumer<String> log)
    {
        WORLD_OVERSEER_HOOK_LOG = log;
    }

    public void SET_WORLD_OVERSEER_HOOK_UPDATE_BROADCAST(BiConsumer<String, String[]> update_broadcast_hook)
    {
        WORLD_OVERSEER_HOOK_UPDATE_BROADCAST = update_broadcast_hook;
    }

    public void SET_WORLD_OVERSEER_VERIFY_NEGOTIATIONS_CALLBACK(BiConsumer<String, Boolean> verify_negotiations_callback)
    {
        WORLD_OVERSEER_CALLBACK_VERIFY_NEGOTIATIONS = verify_negotiations_callback;
    }

    public void SET_WORLD_OVERSEER_HOOK_INVALIDATE(Consumer<String> hook)
    {
        WORLD_OVERSEER_HOOK_INVALIDATE = hook;
    }
}
