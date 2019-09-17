package edu.ozu.drone.client;

import com.google.gson.Gson;
import edu.ozu.drone.client.ui.WorldWatch;
import edu.ozu.drone.utils.JSONWorldWatch;
import edu.ozu.drone.utils.Point;
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

public class AgentHandler {
    private String SERVER = "localhost:3001";
    private String WORLD_ID = "";

    private AgentClient clientRef;
    private AgentClientWebsocketListener websocket;
    private Gson gson;
    private Point AGENT_POSITION;
    private WorldWatch watchUIRef;

    public String AGENT_NAME;

    AgentHandler(AgentClient client)
    {
        clientRef = client;
        AGENT_NAME = client.AGENT_NAME;

        Assert.notNull(client.START, "«START cannot be null»");
        AGENT_POSITION = client.START;

        gson = new Gson();
    }

    /**
     * The function that will be invoked by WebUI when player selects to join a world
     *
     * @param world_id id of the world the agent will join to
     * */
    public void join(String world_id)
    {
        System.out.println("> " + this + " join " + world_id);

        WORLD_ID = world_id.split(":")[1];

        __postJOIN();
        __watch();
    }

    //<editor-fold defaultstate="collapsed" desc="post join">
    @SuppressWarnings("Duplicates")
    private void __postJOIN()
    {
        try
        {
            URL url = new URL("http://" + SERVER + "/join");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; utf-8");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);

            String post_data = "{" +
                    "\"world_id\":\""+WORLD_ID+"\","+
                    "\"agent_id\":\""+clientRef.AGENT_ID+"\","+
                    "\"agent_x\":\""+clientRef.START.x+"\","+
                    "\"agent_y\":\""+clientRef.START.y+"\","+
                    "\"broadcast\":\""+clientRef.getBroadcast()+"\""+
                    "}";

            // write to output stream
            try (OutputStream stream = conn.getOutputStream())
            {
                byte[] bytes = post_data.getBytes(StandardCharsets.UTF_8);
                stream.write(bytes, 0, bytes.length);
            }

            // read response
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            String line;
            StringBuilder response = new StringBuilder();
            while ((line = reader.readLine()) != null)
            {
                response.append(line);
            }

            // ! response should be empty
            System.out.println("> " + this + " __postJOIN:" + response);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    //</editor-fold>

    private void __watch()
    {
        Assert.notNull(watchUIRef, "Watch UI Reference cannot be null");

        try {
            // open websocket
            String ws = "ws://" + SERVER + "/world/" + WORLD_ID + "/" + clientRef.AGENT_ID;
            websocket = new AgentClientWebsocketListener(new URI(ws));

            // add handler
            websocket.setMessageHandler(message -> {
                JSONWorldWatch watch = gson.fromJson(message, JSONWorldWatch.class);

                watchUIRef.draw(watch, AGENT_POSITION);
                handleState(watch);
            });

            // send message
            websocket.sendMessage("ping");
        }
        catch (URISyntaxException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * The function that is invoked after agent joins a world. Allows agent
     * to observe the state of the environment.
     *
     * Essentially handles invocations of other functions depending on the
     * {world_state}:
     * 0 -> join
     * 1 -> collision check
     * 2 -> negotiation step
     * 3 -> move step
     *
     * @param watch: JSONWorldWatch
     * */
    private void handleState(JSONWorldWatch watch)
    {
        switch (watch.world_state)
        {
            case 0: // join
                break;
            case 1:
                // collision check
                if (checkCollision(watch.fov))
                { // negotiation notification
                    notifyNegotiation();
                }
                break;
            case 2: // negotiation time out
                negotiate();
                break;
            case 3: // move and update broadcast
                move();
                break;
            default:
                System.err.println("«unhandled world state:" + watch.world_state + "»");
                break;
        }
    }

    private boolean checkCollision(String[][] fov)
    {
        return false;
    }

    private void notifyNegotiation()
    {
        // todo notify negotiation
    }

    private void negotiate()
    {
        // todo get next paths
        // todo update path
        // todo recalculate
        clientRef.calculatePath(new Point(1,1), new Point(1, 1));
    }

    private void move()
    {
         String[] pos_now = clientRef.path.get(clientRef.time).split("-");
         if (clientRef.time < clientRef.path.size())
         {
             String[] pos_next = clientRef.path.get(clientRef.time + 1).split("-");

             // todo calculate direction
             // todo calculate broadcast

             __postMOVE("", "");
             clientRef.time = clientRef.time + 1;
         }
    }

    //<editor-fold defaultstate="collapsed" desc="post move">
    @SuppressWarnings("Duplicates")
    private void __postMOVE(String direction, String broadcast)
    {
        // post localhost:3001/move payload:
        // direction -> {N, W, E, S}
        try
        {
            URL url = new URL("http://" + SERVER + "/move");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; utf-8");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);

            String post_data = "{" +
                    "\"agent_id\":\""+clientRef.AGENT_ID+"\"," +
                    "\"agent_x\":\""+clientRef.POS.x+"\"," +
                    "\"agent_y\":\""+clientRef.POS.y+"\"," +
                    "\"world_id\":\""+WORLD_ID+"\"," +
                    "\"direction\":\""+direction+"\"," +
                    "\"broadcast\":\""+clientRef.getBroadcast()+"\"" +
                    "}";

            // write to output stream
            try (OutputStream stream = conn.getOutputStream())
            {
                byte[] bytes = post_data.getBytes(StandardCharsets.UTF_8);
                stream.write(bytes, 0, bytes.length);
            }

            // read response
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            String line;
            StringBuilder response = new StringBuilder();
            while ((line = reader.readLine()) != null)
            {
                response.append(line);
            }

            // ! response should be empty
            System.out.println("> " + this + " __postMOVE:" + response);
        }
        catch (IOException err)
        {
            err.printStackTrace();
        }

        // response should match with next path point in line
    }
    //</editor-fold>

    /**
     * Function to be called when world watch disconnects
     * */
    public void leave()
    {
        try
        {
            if (websocket != null)
                websocket.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public String getServer() { return SERVER; }

    public void setWatchUIRef(WorldWatch worldWatch) { this.watchUIRef = worldWatch; }

    // todo handle better later
    public void exit() { System.exit(0); }
}
