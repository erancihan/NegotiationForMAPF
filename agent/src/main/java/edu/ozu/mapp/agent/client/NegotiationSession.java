package edu.ozu.mapp.agent.client;

import com.google.gson.Gson;
import edu.ozu.mapp.agent.Agent;
import edu.ozu.mapp.agent.client.handlers.Negotiation;
import edu.ozu.mapp.utils.Globals;
import edu.ozu.mapp.utils.JSONNegotiationSession;
import edu.ozu.mapp.utils.State;
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
    boolean didDone = false;

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
        connectSocketIO();
    }

    void connectSocketIO()
    {
        didBid = false;
        didDone = false;
        Assert.notNull(session_id, "Session ID cannot be null!");

        try
        {
            NegotiationSocketIO sess = new NegotiationSocketIO(world_id, client.AGENT_ID, session_id);

            //<editor-fold defaultstate="collapsed" desc="Set On Sync Handler">
            sess.setOnSyncHandler(message -> {
                logger.debug(message);

                JSONNegotiationSession json = gson.fromJson(message, JSONNegotiationSession.class);
                client.onReceiveState(new State(json));

                switch (json.state)
                {
                    case "run":
                        if (json.turn.equals("agent:"+client.AGENT_ID))
                        {
                            if (didBid) break;

                            edu.ozu.mapp.utils.Action action = client.onMakeAction();
                            action.bid.apply(this); // TODO change behaviour

                            sess.emitTakeAction(action.toString());

                            didBid = true;
                        } else {
                            didBid = false;
                        }
                        break;
                    case "done":
                        logger.info("negotiation session is done");

                        if (didDone) break;

                        client.acceptLastBids(json);

                        //<editor-fold defaultstate="collapsed" desc="close socket when negotiation done">
                        try { sess.close(); }
                        catch (IOException e) { e.printStackTrace(); }
                        //</editor-fold>

                        client.postNegotiation();

                        didDone = true;
                        break;
                }
            });
            //</editor-fold>
            //<editor-fold defaultstate="collapsed" desc="Set On Joined Handler">
            sess.setOnJoinedHandler(message -> {
                logger.debug(message);

                client.preNegotiation();

                sess.emitReady();
            });
            //</editor-fold>

            sess.start();
        }
        catch (Exception exception) { exception.printStackTrace(); }
    }

    void connectWS()
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
                                edu.ozu.mapp.utils.Action action = client.onMakeAction();
                                action.bid.apply(this); //TODO change behaviour
                                websocket.sendMessage(action.toString());

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
                        client.postNegotiation();
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

    public String getActiveState() {
        return active_state;
    }
}
