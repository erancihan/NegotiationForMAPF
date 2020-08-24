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
            socket = IO.socket("http://localhost:5000/negotiation");
            socket.connect();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public void start()
    {
        logger.info("negotiation socket connection start");

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

    public void setOnSyncHandler(Consumer<String> consumer)
    {
        socket.on("sync_negotiation_state", objects -> consumer.accept(String.valueOf(objects[0])));
    }

    public void setWillJoinHandler(Consumer<String> consumer)
    {
        socket.on("invoke_will_join", objects -> consumer.accept(""));
    }

    public void setOnJoinedHandler(Consumer<String> consumer)
    {
        socket.on("invoke_on_joined", objects -> consumer.accept(String.valueOf(objects[0])));
    }

    public void emitReady()
    {
        socket.emit("negotiation_agent_ready", _SessionID + ":" + _AgentID);
    }

    public void setOnMakeActionHandler(Consumer<String> consumer)
    {
        socket.on("invoke_on_make_action", objects -> consumer.accept(String.valueOf(objects[0])));
    }

    public void emitTakeAction(String string)
    {
        socket.emit("respond_to_make_action", string + "-" + _SessionID);
    }

    public void setOnNegotiationDoneHandler(Consumer<String> consumer)
    {
        socket.on("invoke_on_negotiation_done", objects -> consumer.accept(String.valueOf(objects[0])));
    }

    public void sendMessage(String message)
    {
        _Message = message;
    }
}
