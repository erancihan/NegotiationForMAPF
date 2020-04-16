package edu.ozu.mapp.agent.client;

import edu.ozu.mapp.utils.Globals;
import io.socket.client.IO;
import io.socket.client.Socket;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;

public class NegotiationSocketIO {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(NegotiationSocketIO.class);

    private Socket socket;
    private Timer timer;
    private String _WorldID, _AgentID, _SessionID;
    private String _Message = "";

    public NegotiationSocketIO(String WorldID, String AgentID, String SessionID)
    {
        _WorldID = WorldID;
        _AgentID = AgentID;
        _SessionID = SessionID;

        try {
            socket = IO.socket("http://" + Globals.SERVER + "/negotiation");
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
                    data.put("session_id", _SessionID);
                    data.put("message", _Message);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                socket.emit("negotiation_state", data);
                _Message = ""; // clear message after sending
            }
        }, 0, 100);
    }

    public void close() throws IOException
    {
        logger.info("negotiation socket connection close");
        if (timer != null) timer.cancel();
        if (socket != null) socket.close();
    }

    public void setHandler(Consumer<String> consumer)
    {
        socket.on("sync_negotiation_state", objects -> consumer.accept(String.valueOf(objects[0])));
    }

    public void sendMessage(String message)
    {
        _Message = message;
    }
}
