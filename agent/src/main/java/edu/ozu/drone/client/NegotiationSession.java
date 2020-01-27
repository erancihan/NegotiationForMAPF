package edu.ozu.drone.client;

import com.google.gson.Gson;
import edu.ozu.drone.agent.Agent;
import edu.ozu.drone.utils.ActionType;
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
    private String world_id;
    private String session_id;
    private String active_state = "";
    boolean didBid = false;

    NegotiationSession(String world_id, String session_id, Agent client)
    {
        this.world_id = world_id;
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
            String ws = "ws://" + Globals.SERVER + "/negotiation/" + world_id + "/" + session_id + "/" + client.AGENT_ID;
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
                            // join negotiation session WS
                            // send ready message to socket
                            client.preNegotiation();
                            websocket.sendMessage("agent:" + client.AGENT_ID + "-ready");
                        }
                        break;
                    case "run":
                        if (!active_state.equals("run"))
                        { // register state change
                            active_state = json.state;
                            logger.info("bidding...");
                        }
                        if (json.turn.equals("agent:" + client.AGENT_ID))
                        {// own turn to bid
                            if (!didBid)
                            { // haven't bid yet
                                edu.ozu.drone.utils.Action action = client.onMakeAction();
                                if (action.type == ActionType.ACCEPT)
                                {
                                    websocket.sendMessage(client.AGENT_ID + "-accept");
                                }
                                if (action.type == ActionType.OFFER)
                                {
                                    // todo process action
                                    websocket.sendMessage(client.AGENT_ID + "-bid-" + action.bid);
                                }

                                didBid = true;
                            }
                        } else {
                            didBid = false;
                        }
                        break;
                    case "done":
                        logger.info("negotiation session is done");
                        logger.info("accepted -> " + message.trim());
                        client.acceptLastBids(json);
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
    }
}
