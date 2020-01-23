package edu.ozu.drone.client;

import com.google.gson.Gson;
import edu.ozu.drone.agent.Agent;
import edu.ozu.drone.utils.Globals;
import edu.ozu.drone.utils.JSONNegotiationSession;
import edu.ozu.drone.utils.State;
import org.springframework.util.Assert;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class NegotiationSession
{
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(NegotiationSession.class);
    private Gson gson;

    private Agent client;
    private String session_id;
    private String active_state = "";
    boolean didBid = false;

    NegotiationSession(String session_id, Agent client)
    {
        this.session_id = session_id;
        this.client = client;

        gson = new Gson();

        logger.info("creating new session connection to " + session_id);
    }

    void connect()
    {
        Assert.notNull(session_id, "Session ID cannot be null!");
        active_state = "";
        didBid = false;

        try {
            //!!! sessions contain only one session id for now
            String ws = "ws://" + Globals.SERVER + "/negotiation/" + session_id + "/" + client.AGENT_ID;
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
                JSONNegotiationSession json = gson.fromJson(message, JSONNegotiationSession.class);
                // pass session data to agent -> onReceiveState
                client.onReceiveState(new State(json));
                switch (json.state) {
                    case "join":
                        if (!active_state.equals("join"))
                        { // register state change
                            active_state = json.state;
                            logger.info("joining to negotiation session");
                        }
                        break;
                    case "run":
                        if (!active_state.equals("run"))
                        { // register state change
                            active_state = json.state;
                            logger.info("bidding...");
                        }
                        System.out.println(client.AGENT_ID + ">" + json.turn);
                        if (json.turn.equals("agent:" + client.AGENT_ID))
                        {// own turn to bid
                            if (!didBid)
                            { // haven't bid yet
                                edu.ozu.drone.utils.Action action = client.onMakeAction();
                                // todo process action

                                websocket.sendMessage(client.AGENT_ID + "-bid-" + action);
                                didBid = true;
                            }
                        } else {
                            didBid = false;
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

            // join negotiation session WS
            websocket.sendMessage("agent:" + client.AGENT_ID + "-ready"); // TODO send join message to socket
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }
}
