package edu.ozu.drone.client;

import com.google.gson.Gson;
import edu.ozu.drone.agent.Agent;
import edu.ozu.drone.utils.*;
import org.springframework.util.Assert;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class AgentHandler {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(AgentHandler.class);
    private String AGENT_NAME, SERVER;
    private String WORLD_ID = "";
    private Agent clientRef;
    private WorldWatchWS websocket;
    private Gson gson;
    private Point AGENT_POSITION;
    private String current_state = "";
    private boolean collision_checked;

    AgentHandler(Agent client) {
        Assert.notNull(client.START, "«START cannot be null»");
        Assert.notNull(client.SERVER, "«SERVER cannot be null»");
        Assert.notNull(client.AGENT_ID, "«AGENT_ID cannot be null»");

        clientRef = client;

        AGENT_NAME = client.AGENT_NAME;
        SERVER = client.SERVER;
        AGENT_POSITION = client.START;

        gson = new Gson();
    }

    public String getAgentName() {
        return AGENT_NAME;
    }

    /**
     * The function that will be invoked by WebUI when player selects to join a world
     *
     * @param world_id id of the world the agent will join to
     */
    public void join(String world_id, BiConsumer<JSONWorldWatch, Point> draw) {
        logger.info("joining " + world_id);

        WORLD_ID = world_id.split(":")[1];

        __postJOIN();
        __watch(draw);
    }

    //<editor-fold defaultstate="collapsed" desc="post join">
    @SuppressWarnings("Duplicates")
    private void __postJOIN() {
        try {
            URL url = new URL("http://" + SERVER + "/join");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; utf-8");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);

            String post_data = "{" +
                    "\"world_id\":\"" + WORLD_ID + "\"," +
                    "\"agent_id\":\"" + clientRef.AGENT_ID + "\"," +
                    "\"agent_x\":\"" + clientRef.START.x + "\"," +
                    "\"agent_y\":\"" + clientRef.START.y + "\"," +
                    "\"broadcast\":\"" + clientRef.getBroadcast() + "\"" +
                    "}";

            // write to output stream
            try (OutputStream stream = conn.getOutputStream()) {
                byte[] bytes = post_data.getBytes(StandardCharsets.UTF_8);
                stream.write(bytes, 0, bytes.length);
            }

            // read response
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            String line;
            StringBuilder response = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }

            // ! response should be empty
            logger.info("__postJOIN:" + response);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="watch">
    private void __watch(BiConsumer<JSONWorldWatch, Point> draw) {
        Assert.notNull(draw, "Draw function cannot be null");

        try {
            // open websocket
            String ws = "ws://" + SERVER + "/world/" + WORLD_ID + "/" + clientRef.AGENT_ID;
            websocket = new WorldWatchWS(new URI(ws));

            // add handler
            websocket.setMessageHandler(message -> {
                JSONWorldWatch watch = gson.fromJson(message, JSONWorldWatch.class);

                draw.accept(watch, AGENT_POSITION);
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
            case 1:
                // collision check/broadcast
                if (!collision_checked && hasCollision(watch.fov)) { // negotiation notification
                    notifyNegotiation(watch.fov);
                }
                collision_checked = true;
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

    private boolean hasCollision(String[][] fov) {
        for (String[] item : fov) {
            if (item[2].equals("-")) {
                continue;
            }

            String[] next = item[2].replaceAll("[\\[\\]]", "").split(",");
            for (int i = 0; i < next.length; i++) {
                String a = clientRef.path.get(clientRef.time + i);
                if (a.equals(next[i])) {
                    return true;
                }
            }
        }
        return false;
    }

    //<editor-fold defaultstate="collapsed" desc="notify negotiation">
    private void notifyNegotiation(String[][] fov) { // notify negotiation
        List<String> agents = new ArrayList<>();
        for (String[] agent : fov) {
            agents.add("\"" + agent[0] + "\""); // add agent IDs
        }

        __postNotify(String.valueOf(agents));
    }

    //<editor-fold defaultstate="collapsed" desc="post notify">
    @SuppressWarnings("Duplicates")
    private void __postNotify(String agents) {
        String post_data = "{" +
                "\"world_id\":\"" + WORLD_ID + "\"," +
                "\"agent_id\":\"" + clientRef.AGENT_ID + "\"," +
                "\"agents\":" + agents + "" +
                "}";

        try {
            URL url = new URL("http://" + SERVER + "/negotiation/notify");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; utf-8");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);

            // write to output stream
            try (OutputStream stream = conn.getOutputStream()) {
                byte[] bytes = post_data.getBytes(StandardCharsets.UTF_8);
                stream.write(bytes, 0, bytes.length);
            }

            // read response
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            String line;
            StringBuilder response = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }

            // ! response should be empty
            logger.info("__postNotify:" + response);
        } catch (IOException err) {
            err.printStackTrace();
        }
    }
    //</editor-fold>
    //</editor-fold>

    private void negotiate() {
        String[] sessions = getNegotiationSessions(); // retrieve sessions list
        if (sessions.length > 0) { // negotiating
            try {
                //!!! sessions contain only one session id for now
                String session_id = sessions[0];
                String ws = "ws://" + SERVER + "/negotiation/" + session_id + "/" + clientRef.AGENT_ID;
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
    @SuppressWarnings("Duplicates")
    private String[] getNegotiationSessions() {
        String post_data = "{" +
                "\"world_id\":\"" + WORLD_ID + "\"," +
                "\"agent_id\":\"" + clientRef.AGENT_ID + "\"" +
                "}";

        try {
            URL url = new URL("http://" + SERVER + "/negotiation/sessions");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; utf-8");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);

            // write to output stream
            try (OutputStream stream = conn.getOutputStream()) {
                byte[] bytes = post_data.getBytes(StandardCharsets.UTF_8);
                stream.write(bytes, 0, bytes.length);
            }

            // read response
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            String line;
            StringBuilder response = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }

            JSONSessionsList sessions = gson.fromJson(String.valueOf(response), JSONSessionsList.class);

            return sessions.getSessions();
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        return new String[]{};
    }
    //</editor-fold>

    private void negotiated() {
    }

    private void move() {
        String[] curr = clientRef.path.get(clientRef.time).split("-");
        if (clientRef.time < clientRef.path.size()) {
            String[] next = clientRef.path.get(clientRef.time + 1).split("-");

            String direction = direction(curr, next);
            Assert.isTrue((direction.length() > 0), "«DIRECTION cannot be empty»");

            String broadcast = clientRef.getBroadcast();

            __postMOVE(direction, broadcast);
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
    @SuppressWarnings("Duplicates")
    private void __postMOVE(String direction, String broadcast) {
        // post localhost:3001/move payload:
        // direction -> {N, W, E, S}
        try {
            URL url = new URL("http://" + SERVER + "/move");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; utf-8");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);

            String post_data = "{" +
                    "\"agent_id\":\"" + clientRef.AGENT_ID + "\"," +
                    "\"agent_x\":\"" + clientRef.POS.x + "\"," +
                    "\"agent_y\":\"" + clientRef.POS.y + "\"," +
                    "\"world_id\":\"" + WORLD_ID + "\"," +
                    "\"direction\":\"" + direction + "\"," +
                    "\"broadcast\":\"" + clientRef.getBroadcast() + "\"" +
                    "}";

            // write to output stream
            try (OutputStream stream = conn.getOutputStream()) {
                byte[] bytes = post_data.getBytes(StandardCharsets.UTF_8);
                stream.write(bytes, 0, bytes.length);
            }

            // read response
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            String line;
            StringBuilder response = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }

            // ! response should be empty
            logger.info("__postMOVE:" + response);
        } catch (IOException err) {
            err.printStackTrace();
        }

        // response should match with next path point in line
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
    @SuppressWarnings("Duplicates")
    public void getWorldList(Consumer<String[]> callback) {
        try {
            URL url = new URL("http://" + SERVER + "/worlds");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("GET");

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));

            String il;
            StringBuffer response = new StringBuffer();
            while ((il = in.readLine()) != null) {
                response.append(il);
            }

            Gson gson = new Gson();
            edu.ozu.drone.utils.JSONWorldsList wl = gson.fromJson(String.valueOf(response), edu.ozu.drone.utils.JSONWorldsList.class);

            callback.accept(wl.getWorlds());
        } catch (IOException error) {
            if (error.getClass().getName().equals("java.net.ConnectException")) {
                logger.error("«check server status»");
            } else {
                error.printStackTrace();
            }
        }
    }
    //</editor-fold>
}
