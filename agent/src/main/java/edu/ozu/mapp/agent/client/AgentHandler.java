package edu.ozu.mapp.agent.client;

import com.google.gson.Gson;
import edu.ozu.mapp.agent.Agent;
import edu.ozu.mapp.agent.client.helpers.*;
import edu.ozu.mapp.agent.client.models.Contract;
import edu.ozu.mapp.utils.*;
import org.springframework.util.Assert;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.function.BiConsumer;

public class AgentHandler {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(AgentHandler.class);
    private FileLogger fl;

    private String AGENT_NAME;
    private String WORLD_ID = "";
    private Agent agent;
    private WorldWatchWS websocket;

    private Gson gson;

    private int[] state_flag = new int[2];
    private int is_moving = 1;
    private String conflict_location = "";

    AgentHandler(Agent client) {
        Assert.notNull(client.START, "«START cannot be null»");
        Assert.notNull(client.AGENT_ID, "«AGENT_ID cannot be null»");

        agent = client;

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

    /**
     * The function that will be invoked by WebUI when player selects to join a world
     *
     * @param world_id id of the world the agent will join to
     */
    public void join(String world_id)
    {   // headless join
        this.join(world_id, (arg0, arg1) -> {});
    }

    public void join(String world_id, BiConsumer<JSONWorldWatch, String[]> draw)
    {
        WORLD_ID = world_id.split(":")[1];
        logger.info("joining " + WORLD_ID);

        agent.setWORLD_ID(WORLD_ID);
        agent.dimensions = WorldHandler.GetDimensions(WORLD_ID);

        new Join(agent).join(WORLD_ID);

        fl.setWorldID(WORLD_ID);            // SET AGENT WORLD INFO ON LOGGER
        fl.LogAgentInfo(agent, "JOIN");     // LOG AGENT INFO ON JOIN
        fl.logAgentWorldJoin(agent);    // LOG AGENT JOIN

        __watch(draw);
    }

    //<editor-fold defaultstate="collapsed" desc="Start watching World state :__watch">
    private void __watch(BiConsumer<JSONWorldWatch, String[]> draw)
    {
        Assert.notNull(draw, "Draw function cannot be null");

        try {
            // open websocket
            String ws = "ws://" + Globals.SERVER + "/world/" + WORLD_ID + "/" + agent.AGENT_ID;
            websocket = new WorldWatchWS(new URI(ws));

            // add handler
            websocket.setMessageHandler(message -> {
                JSONWorldWatch watch = gson.fromJson(message, JSONWorldWatch.class);

                draw.accept(watch, agent.GetOwnBroadcastPath());
                handleState(watch);
            });

            // send message
            websocket.sendMessage("ping");
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }
    //</editor-fold>

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
    private void handleState(JSONWorldWatch watch) {
        switch (watch.world_state) {
            case 0: // join
                state_flag = new int[]{0, 0};
                break;
            case 1: // collision check/broadcast
                if (state_flag[0] != 1)
                {
                    checkForCollisions(watch);
                    state_flag[0] = 1;
                    state_flag[1] = watch.time_tick;
                }
                break;
            case 2: // negotiation state
                if (state_flag[0] != 2)
                {
                    negotiate();
                    state_flag[0] = 2;
                    state_flag[1] = watch.time_tick;
                }
                break;
            case 3: // move and update broadcast
                if (state_flag[0] != 3)
                {   // move once
                    move();
                    state_flag[0] = 3;
                    state_flag[1] = watch.time_tick;
                }
                break;
            default:
                logger.error("«unhandled world state:" + watch.world_state + "»");
                break;
        }
    }

    private void checkForCollisions(JSONWorldWatch data)
    {
        String[] agent_ids = getCollidingAgents(data.fov);
        if (agent_ids.length > 1)
        { // my path collides with broadcasted paths!
            logger.debug("notify negotiation > " + Arrays.toString(agent_ids));
            notifyNegotiation(agent_ids);
        }
    }

    private String[] getCollidingAgents(String[][] broadcasts)
    {
        Set<String> agent_ids = new HashSet<>();
        String[] own_path = agent.GetOwnBroadcastPath();

        agent_ids.add("agent:"+ agent.AGENT_ID); // add own data
        for (String[] broadcast : broadcasts)
        {
            if (broadcast[2].equals("-"))
            { // own data
                continue;
            }

            String[] path = broadcast[2].replaceAll("[\\[\\]]", "").split(",");

            ConflictInfo conflict_info = new ConflictCheck().check(own_path, path);
            if (conflict_info.hasConflict)
            {
                // TODO
                // since first Vertex Conflict or Swap Conflict found
                // is immediately returned
                // logging the conflict location here should pose no issue
                if (conflict_info.type == ConflictCheck.ConflictType.SwapConflict)
                    conflict_location = own_path[conflict_info.index] + " -> " + own_path[conflict_info.index + 1];
                else
                    conflict_location = own_path[conflict_info.index];

                logger.info("found " + conflict_info.type + " at " + conflict_location + " | " + Arrays.toString(broadcast));

                return agent_ids.toArray(new String[0]);
            }
        }

        conflict_location = "";
        return agent_ids.toArray(new String[0]);
    }

    //<editor-fold defaultstate="collapsed" desc="Notify Negotiation Participants">
    private void notifyNegotiation(String[] agent_ids)
    {// notify negotiation
        // engage in bi-lateral negotiation session with each of the agents
        // TODO ?? what else is there
        HashMap<String, Object> payload = new HashMap<>();
        payload.put("world_id", WORLD_ID);
        payload.put("agent_id", agent.AGENT_ID);
        payload.put("agents", agent_ids);

        String response = Utils.post("http://localhost:5000/negotiation/notify", payload);

        logger.info("__postNotify" + response);
    }
    //</editor-fold>

    private void negotiate()
    {
        String[] sessions = Negotiation.getSessions(WORLD_ID, agent.AGENT_ID); // retrieve sessions list
        logger.debug("negotiate state > " + Arrays.toString(sessions));
        if (sessions.length > 0)
        {
            for (String sid : sessions)
            {
                if (sid.isEmpty()) { continue; }

                agent.SetConflictLocation(conflict_location);
                NegotiationSession session = new NegotiationSession(WORLD_ID, sid, this);
                session.connect();
            }
        }
    }

    private void move() {
        // check if agent is moving
        // 0: stopped  | out of moves
        // 1: can move | has moves to move
        if (is_moving == 0) return;

        String[] curr = agent.path.get(agent.time).split("-");
        // if next time equals to path size
        // current location is the destination
        if (agent.time + 1 < agent.path.size()) {
            String[] next = agent.path.get(agent.time + 1).split("-");

            String direction = direction(curr, next);
            Assert.isTrue((direction.length() > 0), "«DIRECTION cannot be empty»");

            // make Agent move
            JSONAgent response = Move.move(WORLD_ID, agent.AGENT_ID, agent.POS, direction, agent.getNextBroadcast());

            // update internal clock
            agent.time = agent.time + 1;

            // get next point
            Point next_point = new Point(agent.path.get(Math.min(agent.time, agent.path.size() - 1)).split("-"));

            // validate next location
            Assert.isTrue(
                    (response.agent_x + "-" + response.agent_y).equals(next_point.key), "next point and move action does not match! \n" + response.agent_x + "-" + response.agent_y + " != " + next_point.key + "\n PATH:" + agent.path + "\n"
            );

            // update current position
            agent.POS = next_point;

            agent.OnMove(response);
        } else {
            // no more moves left, agent should stop
            is_moving = 0;

            // let the world know that you are done with it!
            WorldHandler.leave(WORLD_ID, agent.AGENT_ID);
        }
        fl.LogAgentInfo(agent, "MOVE");  // LOG AGENT INFO ON MOVE CALL
    }

    //<editor-fold defaultstate="collapsed" desc="Get Direction of next point">
    private String direction(String[] curr, String[] next) {
        int c_x = Integer.parseInt(curr[0]);
        int c_y = Integer.parseInt(curr[1]);

        int n_x = Integer.parseInt(next[0]);
        int n_y = Integer.parseInt(next[1]);

        if (n_x == c_x) {
            if (n_y - c_y < 0)
                return "N";
            if (n_y - c_y > 0)
                return "S";
        }

        if (n_y == c_y) {
            if (n_x - c_x < 0)
                return "W";
            if (n_x - c_x > 0)
                return "E";
        }

        return "";
    }
    //</editor-fold>

    /**
     * Function to be called when world watch disconnects
     */
    public void leave() {
        try {
            if (websocket != null) {
                websocket.close();
            }
            fl.LogAgentInfo(agent, "LEAVE");  // LOG AGENT INFO ON LEAVE
            fl.logAgentWorldLeave(agent);           // LOG AGENT LEAVING
            WorldHandler.leave(WORLD_ID, agent.AGENT_ID);
        } catch (IOException e) {
            e.printStackTrace();
        }
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

    public void OnReceiveState(State state)
    {
        // New state received
        // fetch current Contract
        Contract contract = Negotiation.getContract(agent);

        if (contract != null) agent.history.put(contract);

        agent.onReceiveState(state);
    }

    /**
     * Invoked only when negotiation is initiated, and agent
     * is about to join. (Before {@link #PrepareContract(NegotiationSession)})
     *
     * Negotiation session status should be "join" for this
     * function to be invoked
     *
     * Fills the empty contract with necessary information.
     *
     * Mark that a new session has started
     * */
    @SuppressWarnings("UnusedReturnValue")
    public Contract PrepareContract(NegotiationSession session)
    {
        // clear negotiation result checker
        // todo better handle
        // current setup assumes that there will be only single instance of
        // negotiation that agent participates at any given time
        agent.negotiation_result = -1;
        return Contract.Create(agent, session);
    }

    public void PreNegotiation(String session_id, State state)
    {
        agent.history.setCurrentNegotiationID(session_id);
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

    public Action OnMakeAction() {
        return agent.onMakeAction();
    }

    @SuppressWarnings("Duplicates")
    //<editor-fold defaultstate="collapsed" desc="Accept Last Bids">
    public void AcceptLastBids(JSONNegotiationSession json) {
        // get contract
        Contract contract = Negotiation.getContract(agent);
        logger.debug("AcceptLastBids:"+contract);
        logger.debug("          json:"+json);

        List<String> new_path = new ArrayList<>();

        // use contract to apply select paths
        // if 'x' is self, update planned path
        if (contract.x.equals(agent.AGENT_ID)) {
            // WIN condition
            logger.debug("x is self | {contract.x:"+contract.x + " == a_id:" + agent.AGENT_ID + "}");

            String[] Ox = contract.Ox.replaceAll("([\\[\\]]*)", "").split(",");

            logger.debug("{current POS:" + agent.POS + " == Ox[0]:" + Ox[0] + "}");
            Assert.isTrue(agent.POS.equals(new Point(Ox[0].split("-"))), "");

            // acknowledge negotiation result and calculate from its last point to the goal
            Point end = new Point(Ox[Ox.length - 1].split("-"));
            // recalculate path starting from the end point of agreed path
            logger.debug("{accepted_path:" + Arrays.toString(Ox) + "}");
            logger.debug("{calculating path from:" + end + " to:" + agent.DEST + " }");
            List<String> rest = agent.calculatePath(end, agent.DEST);
            logger.debug("{rest: " + Arrays.toString(rest.toArray()) + " }");

            // ...glue them together
            new_path = new ArrayList<>();
            for (int idx = 0; idx < agent.path.size() && !agent.path.get(idx).equals(agent.POS.key); idx++)
            {   // prepend path so far until current POS
                // for history purposes
                new_path.add(agent.path.get(idx));
            }
            new_path.add(agent.POS.key); // add current POS
            for (int idx = 0; idx < Ox.length; idx++)
            {   // add accepted paths
                if (idx == 0 && Ox[idx].equals(agent.POS.key))
                {   // skip if first index is current POS, as it is already added
                    continue;
                }
                new_path.add(Ox[idx]);
            }

            // ensure that connection points match
            Assert.isTrue(
                    new_path.get(new_path.size() - 1).equals(rest.get(0)),
                    "Something went wrong while accepting last bids!"
            );

            // merge...
            for (int idx = 1; idx < rest.size(); idx++)
            {
                new_path.add(rest.get(idx));
            }
            agent.winC++;
        } else {
            // else use 'Ox' & others as constraint & re-calculate path
            // LOSE condition
            logger.debug("x is not self | {contract.x:"+contract.x + " != a_id:" + agent.AGENT_ID + "}");

            String[] Ox = contract.Ox.replaceAll("([\\[\\]]*)", "").split(",");

            // create constraints
            ArrayList<String[]> constraints = new ArrayList<>();
            for (int i = 0; i < Ox.length; i++)
            {   // Add Ox as constraint
                constraints.add(new String[]{Ox[i], String.valueOf(agent.time + i)});
            }
            // TODO add from FoV

            List<String> rest = AStar.calculateWithConstraints(agent.POS, agent.DEST, constraints.toArray(new String[0][0]));

            new_path = new ArrayList<>();
            for (int idx = 0; idx < agent.path.size() && !agent.path.get(idx).equals(agent.POS.key); idx++)
            {   // prepend path so far until current POS
                // for history purposes
                new_path.add(agent.path.get(idx));
            }

            // ensure that connection points match
            Assert.isTrue(agent.POS.key.equals(rest.get(0)), "Something went wrong while accepting last bids!");
            Assert.isTrue(agent.DEST.key.equals(rest.get(rest.size() - 1)), "Something went wrong while accepting last bids!");

            // merge...
            // since current POS is already in 'rest'@0, we can just add it
            new_path.addAll(rest);
            agent.loseC++;
        }

        // update global path
        logger.debug(agent.AGENT_ID + "{path:" + agent.path + "}");
        agent.path = new_path;
        logger.debug(agent.AGENT_ID + "{path:" + agent.path + "}");

        WorldHandler.doBroadcast(WORLD_ID, agent.AGENT_ID, agent.GetOwnBroadcastPath());
        agent.OnAcceptLastBids(json);
    }
    //</editor-fold>

    public void PostNegotiation()
    {
        agent.PostNegotiation();
    }

    public void LogNegotiationOver(String bidding_agent, String session_id)
    {
        // todo it should be possible to merge this function with another
        agent.LogNegotiationOver(bidding_agent, session_id);
    }
}
