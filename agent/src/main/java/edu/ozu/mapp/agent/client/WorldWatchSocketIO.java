package edu.ozu.mapp.agent.client;

import edu.ozu.mapp.utils.Globals;
import io.socket.client.IO;
import io.socket.client.Socket;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;

public class WorldWatchSocketIO {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(WorldWatchSocketIO.class);

    private Socket socket;
    private Timer timer;
    private String _WorldID, _AgentID;
    private String _Message = "";

    public WorldWatchSocketIO(String WorldID, String AgentID)
    {
        _WorldID = WorldID;
        _AgentID = AgentID;

        try {
            socket = IO.socket("http://" + Globals.SERVER + "/world");
            socket.connect();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                JSONObject data = new JSONObject();

                try {
                    data.put("world_id", _WorldID);
                    data.put("agent_id", _AgentID);
                    data.put("message", _Message);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                socket.emit("world_state", data);
                _Message = ""; // clear message after sending
            }
        }, 0, 100);
    }

    public void close() throws IOException
    {
        logger.info("world socket connection close");
        if (timer != null) timer.cancel();
        if (socket != null) socket.close();
    }

    public void setMessageHandler(Consumer<String> consumer)
    {
        socket.on("sync_world_state", objects -> {
            System.out.println(Arrays.deepToString(objects));
            consumer.accept(String.valueOf(objects[0]));
        });
    }

    public void sendMessage(String message)
    {
        _Message = message;
    }
}
