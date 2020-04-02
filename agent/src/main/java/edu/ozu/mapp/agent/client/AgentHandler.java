package edu.ozu.mapp.agent.client;

import com.google.gson.Gson;
import edu.ozu.mapp.agent.Agent;
import edu.ozu.mapp.agent.client.handlers.Join;
import edu.ozu.mapp.agent.client.handlers.Move;
import edu.ozu.mapp.agent.client.handlers.Negotiation;
import edu.ozu.mapp.utils.*;
import org.springframework.util.Assert;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;

public class AgentHandler {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(AgentHandler.class);
    private String AGENT_NAME;
    private String WORLD_ID = "";
    private Agent clientRef;
    private WorldWatchWS websocket;
    private Gson gson;
    private int[] state_flag = new int[2];

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
    public void join(String world_id)
    {   // headless join
        this.join(world_id, (arg0, arg1) -> {});
    }

    public void join(String world_id, BiConsumer<JSONWorldWatch, String[]> draw)
    {
        WORLD_ID = world_id.split(":")[1];
        logger.info("joining " + WORLD_ID);

        clientRef.setWORLD_ID(WORLD_ID);

        Join.join(WORLD_ID, clientRef.AGENT_ID, clientRef.START, clientRef.getBroadcast());
        __watch(draw);
    }

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
        String[] own_path = clientRef.getBroadcastArray();

        agent_ids.add("agent:"+clientRef.AGENT_ID); // add own data
        for (String[] broadcast : broadcasts)
        {
            if (broadcast[2].equals("-"))
            { // own data
                continue;
            }

            String[] path = broadcast[2].replaceAll("[\\[\\]]", "").split(",");
            // check Vertex Conflict
            for (int i = 0; i < path.length && i < own_path.length; i++)
            {
                if (path[i].equals(own_path[i]))
                {
                    agent_ids.add(broadcast[0]);
                    logger.info("found a Vertex Conflict at " + path[i] + "|" + Arrays.toString(broadcast));

                    return agent_ids.toArray(new String[0]);
                }
            }
            /* disable swap conflict for now
            // check Swap Conflict
            // get reverse of path
            String[] path_reverse = new String[path.length];
            for (int i = 0; i < path.length; i++)
                path_reverse[i] = path[path.length - 1 - i];
            // check match
            for (int i = 0; i < path_reverse.length; i++)
            {
                if (path_reverse[i].equals(own_path[i]))
                {
                    agent_ids.add(broadcast[0]);
                    logger.info("found a Swap Conflict at " + path_reverse[i] + "|" + Arrays.toString(broadcast));

                    return agent_ids.toArray(new String[0]);
                }
            }
            */
        }

        return agent_ids.toArray(new String[0]);
    }

    //<editor-fold defaultstate="collapsed" desc="Notify Negotiation Participants">
    private void notifyNegotiation(String[] agent_ids)
    {// notify negotiation
        // engage in bi-lateral negotiation session with each of the agents
        // TODO ?? what else is there
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
        String[] sessions = Negotiation.getSessions(WORLD_ID, clientRef.AGENT_ID); // retrieve sessions list
        logger.debug("negotiate state > " + Arrays.toString(sessions));
        if (sessions.length > 0)
        {
            for (String sid : sessions)
            {
                if (sid.isEmpty()) { continue; }

                NegotiationSession session = new NegotiationSession(WORLD_ID, sid, clientRef);
                session.connect();
            }
        }
    }

    private void move() {
        String[] curr = clientRef.path.get(clientRef.time).split("-");
        if (clientRef.time + 1 < clientRef.path.size()) {
            String[] next = clientRef.path.get(clientRef.time + 1).split("-");

            String direction = direction(curr, next);
            Assert.isTrue((direction.length() > 0), "«DIRECTION cannot be empty»");

            clientRef.move(Move.move(WORLD_ID, clientRef.AGENT_ID, clientRef.POS, direction, clientRef.getNextBroadcast()));
        }
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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // todo handle better later
    public void exit() {
        leave();
        System.exit(0);
    }
}
