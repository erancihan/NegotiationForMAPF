package edu.ozu.mapp.agent.client;

import com.google.gson.Gson;
import edu.ozu.mapp.agent.Agent;
import edu.ozu.mapp.agent.client.helpers.ConflictCheck;
import edu.ozu.mapp.agent.client.helpers.ConflictInfo;
import edu.ozu.mapp.agent.client.helpers.FileLogger;
import edu.ozu.mapp.agent.client.models.Contract;
import edu.ozu.mapp.dataTypes.Constraint;
import edu.ozu.mapp.dataTypes.ReturnType;
import edu.ozu.mapp.system.DATA_REQUEST_PAYLOAD_WORLD_JOIN;
import edu.ozu.mapp.system.DATA_REQUEST_PAYLOAD_WORLD_MOVE;
import edu.ozu.mapp.utils.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.util.Assert;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class AgentHandler {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(AgentHandler.class);
    private FileLogger fl;

    private String AGENT_NAME;
    private String WORLD_ID = "";
    private Agent agent;

    private Gson gson;

    private int is_moving = 1;
    private String conflict_location = "";

    private Function<DATA_REQUEST_PAYLOAD_WORLD_JOIN, String[]> WORLD_HANDLER_JOIN;
    private BiConsumer<String, String[]>                        WORLD_HANDLER_COLLISION_CHECK_DONE;
    private Function<String, Contract>                          WORLD_OVERSEER_HOOK_GET_NEGOTIATION_CONTRACT;
    private Function<String, String[]>                          WORLD_HANDLER_GET_NEGOTIATION_SESSIONS;
    private BiConsumer<String, AgentHandler>                    WORLD_OVERSEER_JOIN_NEGOTIATION_SESSION;
    private BiConsumer<String, String>                          WORLD_OVERSEER_NEGOTIATED;
    private BiConsumer<AgentHandler, DATA_REQUEST_PAYLOAD_WORLD_MOVE> WORLD_OVERSEER_MOVE;
    private Consumer<AgentHandler>                              WORLD_OVERSEER_HOOK_LEAVE;
    private Consumer<String>                                    WORLD_OVERSEER_HOOK_LOG;

    private String[][] fov;

    AgentHandler(Agent client) {
        Assert.notNull(client.START, "«START cannot be null»");
        Assert.notNull(client.AGENT_ID, "«AGENT_ID cannot be null»");

        agent = client;
        agent.SetHandlerRef(this);

        // init file logger
        fl = new FileLogger().CreateAgentLogger(agent.AGENT_ID);

        AGENT_NAME = client.AGENT_NAME;

        gson = new Gson();
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
        payload.AGENT_NAME      = getAgentName();
        payload.AGENT_ID        = agent.AGENT_ID;
        payload.LOCATION        = agent.START;
        payload.BROADCAST       = agent.GetOwnBroadcastPath();
        payload.INIT_TOKEN_C    = agent.initial_tokens;

        String[] response = WORLD_HANDLER_JOIN.apply(payload);
        WORLD_ID = response[0];

        logger.info("joining " + WORLD_ID + " | serverless");

        agent.setWORLD_ID(WORLD_ID);
        agent.dimensions = response[1];

        fl.setWorldID(WORLD_ID);            // SET AGENT WORLD INFO ON LOGGER
        fl.LogAgentInfo(agent, "JOIN");     // LOG AGENT INFO ON JOIN
        fl.logAgentWorldJoin(agent);    // LOG AGENT JOIN

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
    private final Lock handle_state_run_once_at_a_time_lock = new ReentrantLock();
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

                    collision_check(watch);
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

    private void collision_check(JSONWorldWatch data)
    {
        if (is_moving == 0) return;

        ReturnType result = getCollidingAgents(data.fov);
        switch (result.type) {
            case COLLISION:
                // There is a collision in path! Resolve this first
                logger.debug(getAgentName() + " | notify negotiation > " + Arrays.toString(result.agent_ids));
                state_flag[0] = 1;
                WORLD_HANDLER_COLLISION_CHECK_DONE.accept(getAgentName(), result.agent_ids);
                break;
            case OBSTACLE:
                // There is an obstacle in the path! Update route according to constraints
                logger.debug(getAgentName() + " | found obstacle");
                // Re-calculate path starting from here on out
                agent.path = update_agent_path_from_pos_to_dest();
                break;
            case NONE:
            default:
                state_flag[0] = 1;
                WORLD_HANDLER_COLLISION_CHECK_DONE.accept(getAgentName(), new String[]{agent.AGENT_ID});
        }
    }

    private ReturnType getCollidingAgents(String[][] data)
    {
        List<String[]> broadcasts = Arrays.stream(data).sorted().collect(Collectors.toList());
        Set<String> agent_ids = new HashSet<>();
        String[] own_path = agent.GetOwnBroadcastPath();

        agent_ids.add(agent.AGENT_ID); // add own data
        for (String[] broadcast : broadcasts)
        {
            if (broadcast[2].equals("-"))
            { // own data
                continue;
            }

            if (broadcast[2].equals("inf"))
            {   // OBSTACLE IN FoV
                if (Arrays.asList(own_path).contains(broadcast[1]))
                {   // OBSTACLE IS IN WAY
                    agent.constraints.add(new Constraint(new Point(broadcast[1], "-")));

                    return new ReturnType(ReturnType.Type.OBSTACLE);
                }
            }

            String[] path = broadcast[2].replaceAll("[\\[\\]]", "").split(",");
            ConflictInfo conflict_info = new ConflictCheck().check(own_path, path);
            if (conflict_info.hasConflict)
            {
                agent_ids.add(broadcast[0]);
                // TODO
                // since first Vertex Conflict or Swap Conflict found
                // is immediately returned
                // logging the conflict location here should pose no issue
                if (conflict_info.type == ConflictCheck.ConflictType.SwapConflict)
                    conflict_location = own_path[conflict_info.index] + " -> " + own_path[conflict_info.index + 1];
                else
                    conflict_location = own_path[conflict_info.index];

                logger.info("found " + conflict_info.type + " at " + conflict_location + " | " + Arrays.toString(broadcast));

                ReturnType ret = new ReturnType(ReturnType.Type.COLLISION);
                ret.agent_ids = agent_ids.toArray(new String[0]);

                return ret;
            }
        }

        conflict_location = "";
        return new ReturnType();
    }

    private void negotiate()
    {
        if (is_moving == 0) return;

        String[] sessions = WORLD_HANDLER_GET_NEGOTIATION_SESSIONS.apply(getAgentName());
        logger.debug(getAgentName() + " | negotiate state > " + Arrays.toString(sessions));
        if (sessions.length > 0)
        {
            for (String sid : sessions)
            {
                if (sid.isEmpty()) { continue; }

                agent.SetConflictLocation(conflict_location);

                WORLD_OVERSEER_JOIN_NEGOTIATION_SESSION.accept(sid, this);

                logger.debug(getAgentName() + " | " + sid);
            }
        } else {
            WORLD_OVERSEER_NEGOTIATED.accept(getAgentName(), "");
        }
    }

    private void move() {
        // check if agent is moving
        // 0: stopped  | out of moves
        // 1: can move | has moves to move
        if (is_moving == 0) return;

        if (agent.time + 1 < agent.path.size()) {
            System.out.println("moving " + getAgentName());

            // make Agent move
            DATA_REQUEST_PAYLOAD_WORLD_MOVE payload = new DATA_REQUEST_PAYLOAD_WORLD_MOVE();
            payload.AGENT_NAME = getAgentName();
            payload.CURRENT_LOCATION = agent.POS;
            payload.NEXT_LOCATION    = new Point(agent.path.get(agent.time + 1), "-");
            payload.BROADCAST        = agent.GetNextBroadcast();

            WORLD_OVERSEER_MOVE.accept(this, payload);
        } else {
            System.out.println(getAgentName() + " stopping t:" + agent.time + " path: " + agent.path);
            // no more moves left, agent should stop
            is_moving = 0;

            // let the world know that you are done with it!
            leave();
        }
        fl.LogAgentInfo(agent, "MOVE");  // LOG AGENT INFO ON MOVE CALL
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
        agent.POS = next_point;
        logger.debug("moving to " + next_point.key);

        agent.OnMove(response);
    }

    /**
     * Function to be called when world watch disconnects
     */
    public void leave()
    {
        fl.LogAgentInfo(agent, "LEAVE");  // LOG AGENT INFO ON LEAVE
        fl.logAgentWorldLeave(agent);           // LOG AGENT LEAVING

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

        agent.PreNegotiation(state);
    }

    public void LogPreNegotiation(String session_id)
    {
        // todo it should be possible to merge this function with another
        agent.logNegoPre(session_id);
    }

    public void LogNegotiationState(String bidding_agent)
    {
        agent.LogNegotiationState(bidding_agent);
    }

    public void LogNegotiationState(String prev_bidding_agent, Action action)
    {
        agent.LogNegotiationState(prev_bidding_agent, action);
    }

    public Action OnMakeAction(Contract contract)
    {
        return agent.onMakeAction(contract);
    }

    public void AcceptLastBids(JSONNegotiationSession json)
    {
        // get contract
        Contract contract = agent.GetContract();

        logger.debug("AcceptLastBids:"+contract);
        logger.debug("          json:"+json);

        AcceptLastBids(contract);

        agent.OnAcceptLastBids(json);
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
    }

    @NotNull
    private List<String> handle_win_condition(Contract contract) {
        // WIN condition
        logger.debug(String.format("%s | WON | %s", agent.AGENT_ID, contract.print()));
        logger.debug(String.format("%s | current location : %s", agent.AGENT_ID, agent.POS));

        Point[] Ox = contract.GetOx();

        logger.debug(String.format("%s | ASSERT POS{%s} == Ox[0]{%s}", agent.AGENT_ID, agent.POS.key, Ox[0]));
        Assert.isTrue(agent.POS.equals(Ox[0]), "inconsistent bid location");

        logger.debug(String.format("%s | ACCEPTED PATH %s", agent.AGENT_ID, Arrays.toString(Ox)));

        List<String> path_next = new ArrayList<>();

        // acknowledge negotiation result.
        // since i have won, the path that i have chosen for my self is locked

        // add past path locations
        for (int idx = 0; idx < agent.path.size() && !agent.path.get(idx).equals(agent.POS.key); idx++)
        {
        logger.debug(String.format("%s | PATH UPDATE | ADDING %s %s", agent.AGENT_ID, idx, agent.path.get(idx)));
            path_next.add(agent.path.get(idx));
        }
        // add Ox
        for (int idx = 0; idx < Ox.length; idx++)
        {
            path_next.add(Ox[idx].key);
        }
        // calculate path from last to destination
        // i do not have to worry about opponent's path since i won
        Point end = new Point(path_next.get(path_next.size()-1), "-");

        logger.debug(String.format("%s | CALCULATING REST OF THE PATH", agent.AGENT_ID));
        logger.debug(String.format("%s | FROM %s TO %s", agent.AGENT_ID, end, agent.DEST));

        List<String> rest = agent.calculatePath(end, agent.DEST);

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
        update_agent_constraints(contract);
        logger.debug(String.format("%s | LOST | %s", agent.AGENT_ID, contract.print()));
        agent.loseC++;

        return update_agent_path_from_pos_to_dest();
    }

    /**
     * Run when agent is lost to set accepted Ox points as constraints
     * */
    private void update_agent_constraints(Contract contract)
    {
        // create constraints, add Ox as constraint
        int i = 0;
        for (Point point : contract.GetOx())
        {
            agent.constraints.add(new Constraint(point, agent.time + i));
            logger.debug(String.format("%s | ADDED CONSTRAINT %s:%s", agent.AGENT_ID, point, agent.time + i));
            i++;
        }
    }
    //</editor-fold>

    @NotNull
    private List<String> update_agent_path_from_pos_to_dest() {
        logger.debug(String.format("%s | current location : %s", agent.AGENT_ID, agent.POS));

        List<String> path_next = new ArrayList<>();
        // add path up until now
        for (int idx = 0; idx < agent.path.size() && !agent.path.get(idx).equals(agent.POS.key); idx++)
        {
            path_next.add(agent.path.get(idx));
        }

        // calculate rest of the path
        List<String> rest = agent.calculatePath(agent.POS, agent.DEST, new HashMap<>());

        // ensure that connection points match
        Assert.isTrue(agent.POS.key.equals(rest.get(0)), "Something went wrong while accepting last bids!");
        Assert.isTrue(agent.DEST.key.equals(rest.get(rest.size() - 1)), "Something went wrong while accepting last bids!");

        // merge...
        // since current POS is already in 'rest'@0, we can just add it
        path_next.addAll(rest);

        return path_next;
    }

    public void PostNegotiation(String session_id)
    {
        if (WORLD_OVERSEER_NEGOTIATED != null) {
            WORLD_OVERSEER_NEGOTIATED.accept(getAgentName(), session_id);
        }

        agent.PostNegotiation();
    }

    public void LogNegotiationOver(String bidding_agent, String session_id)
    {
        // todo it should be possible to merge this function with another
        agent.LogNegotiationOver(bidding_agent, session_id);
    }

    public int UpdateTokenCountBy(int i)
    {
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

    /** ================================================================================================================ **/

    public void SetJoinCallback(Function<DATA_REQUEST_PAYLOAD_WORLD_JOIN, String[]> function)
    {
        WORLD_HANDLER_JOIN = function;
    }

    public void SET_COLLISION_CHECK_DONE(BiConsumer<String, String[]> function)
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
}
