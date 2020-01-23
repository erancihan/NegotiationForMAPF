package edu.ozu.drone.client;

import com.google.gson.Gson;
import edu.ozu.drone.agent.Agent;
import edu.ozu.drone.utils.*;
import org.springframework.util.Assert;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class AgentHandler {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(AgentHandler.class);
    private String AGENT_NAME;
    private String WORLD_ID = "";
    private Agent clientRef;
    private WorldWatchWS websocket;
    private Gson gson;
    private String current_state = "";
    private boolean collision_checked;

    AgentHandler(Agent client) {
        Assert.notNull(client.START, "«START cannot be null»");
        Assert.notNull(client.AGENT_ID, "«AGENT_ID cannot be null»");

        clientRef = client;

        AGENT_NAME = client.AGENT_NAME;

        gson = new Gson();
    }

    public String getAgentName()
    {
        return AGENT_NAME;
    }

    public String getID()
    {
        return clientRef.AGENT_ID;
    }

    /**
     * The function that will be invoked by WebUI when player selects to join a world
     *
     * @param world_id id of the world the agent will join to
     */
    public void join(String world_id, BiConsumer<JSONWorldWatch, String[]> draw)
    {
        WORLD_ID = world_id.split(":")[1];
        logger.info("joining " + WORLD_ID);

        __postJoin();
        __watch(draw);
    }

    //<editor-fold defaultstate="collapsed" desc="Join to World :__postJoin">
    private void __postJoin()
    {
        HashMap<String, Object> payload = new HashMap<>();
        payload.put("world_id", WORLD_ID);
        payload.put("agent_id", clientRef.AGENT_ID);
        payload.put("agent_x", String.valueOf(clientRef.START.x));
        payload.put("agent_y", String.valueOf(clientRef.START.y));
        payload.put("broadcast", clientRef.getBroadcast());

        String response = Utils.post("http://" + Globals.SERVER + "/join", payload);

        logger.info("__postJoin:" + WORLD_ID + "> " + response);
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Start watching World state :__watch">
    private void __watch(BiConsumer<JSONWorldWatch, String[]> draw)
    {
        Assert.notNull(draw, "Draw function cannot be null");

        try {
            // open websocket
            String ws = "ws://" + Globals.SERVER + "/world/" + WORLD_ID + "/" + clientRef.AGENT_ID;
            websocket = new WorldWatchWS(new URI(ws));

            // add handler
            websocket.setMessageHandler(message -> {
                JSONWorldWatch watch = gson.fromJson(message, JSONWorldWatch.class);

                draw.accept(watch, clientRef.getBroadcastArray());
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
                collision_checked = false;
                break;
            case 1: // collision check/broadcast
                if (!collision_checked)
                {
                    checkForCollisions(watch);
                    collision_checked = true;
                }
                break;
            case 2: // negotiation state
                negotiate();
                break;
            case 3: // move and update broadcast
                move();
                collision_checked = false;
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
            notifyNegotiation(agent_ids);
        }
    }

    private String[] getCollidingAgents(String[][] broadcasts)
    {
        ArrayList<String> agent_ids = new ArrayList<>();
        String[] own_path = clientRef.getBroadcastArray();

        for (String[] broadcast : broadcasts)
        {
            if (broadcast[2].equals("-"))
            { // add self
                agent_ids.add(broadcast[0]);
                continue;
            }

            String[] path = broadcast[2].replaceAll("[\\[\\]]", "").split(",");
            // check Vertex Conflict
            for (int i = 0; i < path.length && i < own_path.length; i++)
            {
                if (path[i].equals(own_path[i]))
                    agent_ids.add(broadcast[0]);
            }
            // TODO check Swap Conflict
        }

        return agent_ids.toArray(new String[0]);
    }

    //<editor-fold defaultstate="collapsed" desc="Notify Negotiation Participants">
    private void notifyNegotiation(String[] agent_ids)
    {// notify negotiation
        // engage in bi-lateral negotiation session with each of the agents
        // TODO
        HashMap<String, Object> payload = new HashMap<>();
        payload.put("world_id", WORLD_ID);
        payload.put("agent_id", clientRef.AGENT_ID);
        payload.put("agents", agent_ids);

        String response = Utils.post("http://" + Globals.SERVER + "/negotiation/notify", payload);

        logger.info("__postNotify" + response);
    }
    //</editor-fold>

    private void negotiate()
    {
        String[] sessions = getNegotiationSessions(); // retrieve sessions list
        if (sessions.length > 0) { // negotiating
            try {
                //!!! sessions contain only one session id for now
                String session_id = sessions[0];
                String ws = "ws://" + Globals.SERVER + "/negotiation/" + session_id + "/" + clientRef.AGENT_ID;
                NegotiationWS websocket = new NegotiationWS(new URI(ws));

                /* add handler
                 * Message format:
                 *  agent_count: <integer>                      | number of agents
                 *  bid_order: [agent_0, agent_1, ..., agent_i] | list of agent IDs.
                 *  bids     : [bid_agent_0, ..., bid_agent_i]  | list of bids of agents with IDs given
                 *  state    : {join|run|done}                  | state of the negotiation session
                 *  turn     : "agent_id"                       | ID of agent who's turn it is to bid
                 * */
                websocket.setHandler(message -> {
                    System.out.println(message);

                    JSONNegotiationSession json = gson.fromJson(message, JSONNegotiationSession.class);
                    // pass session data to agent -> onReceiveState
                    clientRef.onReceiveState(new State(json));
                    switch (json.state) {
                        case "join":
                            if (!current_state.equals("join"))
                            { // check state change
                                current_state = json.state;
                                logger.info("joining to negotiation session");
                            }
                            break;
                        case "run":
                            if (!current_state.equals("run"))
                            { // check state change
                                current_state = json.state;
                                logger.info("bidding...");

                                if (json.turn.equals(clientRef.AGENT_ID))
                                { // own turn to bid
                                    edu.ozu.drone.utils.Action action = clientRef.onMakeAction();
                                    websocket.sendMessage(String.valueOf(action));
                                }
                            }
                            break;
                        case "done":
                            logger.info("negotiation session is done");
                            //<editor-fold defaultstate="collapsed" desc="close socket when negotiation done">
                            try {
                                websocket.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            //</editor-fold>
                            break;
                        default:
                            logger.error("unexpected state, contact DEVs");
                            System.exit(1);
                    }
                });
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
            // join negotiation session WS
            websocket.sendMessage("agent:" + clientRef.AGENT_ID + ":ready"); // TODO send join message to socket
            // on close
            // todo get next paths
            // todo update path
            // todo recalculate
        }
//         else:
//             done
//
//        clientRef.calculatePath(new Point(1,1), new Point(1, 1));
    }

    //<editor-fold defaultstate="collapsed" desc="retrieve list of negotiation session IDs of agent">
    /**
     * Retrieves list of negotiation session IDs that agent will attend
     */
    private String[] getNegotiationSessions() {
        HashMap<String, Object> payload = new HashMap<>();
        payload.put("world_id", WORLD_ID);
        payload.put("agent_id", clientRef.AGENT_ID);

        String response = Utils.post("http://" + Globals.SERVER + "/negotiation/sessions", payload);

        JSONSessionsList sessions = gson.fromJson(response, JSONSessionsList.class);
        return sessions.getSessions();
    }
    //</editor-fold>

    private void move() {
        String[] curr = clientRef.path.get(clientRef.time).split("-");
        if (clientRef.time < clientRef.path.size()) {
            String[] next = clientRef.path.get(clientRef.time + 1).split("-");

            String direction = direction(curr, next);
            Assert.isTrue((direction.length() > 0), "«DIRECTION cannot be empty»");

            __postMOVE(direction);
            clientRef.time = clientRef.time + 1;
        }
    }

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

    //<editor-fold defaultstate="collapsed" desc="post move">
    private void __postMOVE(String direction)
    {
        // post localhost:3001/move payload:
        // direction -> {N, W, E, S}
        HashMap<String, Object> payload = new HashMap<>();
        payload.put("agent_id", clientRef.AGENT_ID);
        payload.put("world_id", WORLD_ID);
        payload.put("agent_x", String.valueOf(clientRef.POS.x));
        payload.put("agent_y", String.valueOf(clientRef.POS.y));
        payload.put("direction", direction);
        payload.put("broadcast", clientRef.getBroadcast());

        String response = Utils.post("http://" + Globals.SERVER + "/move", payload);

        // response should match with next path point in line
        logger.info("__postMOVE:" + response);
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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // todo handle better later
    public void exit() {
        System.exit(0);
    }

    //<editor-fold defaultstate="collapsed" desc="get world list">
    public void getWorldList(Consumer<String[]> callback)
    {
        String response = Utils.get("http://" + Globals.SERVER + "/worlds");
        edu.ozu.drone.utils.JSONWorldsList wl = gson.fromJson(response, edu.ozu.drone.utils.JSONWorldsList.class);
        callback.accept(wl.getWorlds());
    }
    //</editor-fold>
}
