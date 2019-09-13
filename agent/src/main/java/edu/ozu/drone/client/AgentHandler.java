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
        // todo
        // get path [0, 2]

        // join
        // post localhost:3001/join payload:{world_id, agent_id, agent_x, agent_y}

        // @response: watch()
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
                    "\"agent_y\":\""+clientRef.START.y+"\""+
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
