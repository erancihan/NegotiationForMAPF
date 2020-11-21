package edu.ozu.mapp.agent.client;

import com.google.gson.Gson;
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

    private AgentHandler handler;
    private String world_id;
    private String session_id;
    private String active_state = "";
    private String bidding_agent = "";

    boolean didJoin = false;
    boolean didBid = false;
        int _didBid = -1;
    boolean didDone = false;

    NegotiationSession(String world_id, String session_id, AgentHandler handler)
    {
        this.world_id = world_id;
        this.session_id = session_id;
        this.handler = handler;

        gson = new Gson();
    }

    void connect()
    {
        logger.info("connecting to " + session_id + " as " + handler.GetAgentID());
//        connectSocketIO();
        connectWS();
    }

    //<editor-fold defaultstate="collapsed" desc="SocketIO Handler Func">
    /*
    void connectSocketIO()
    {
        _didBid = -1;
        didDone = false;
        Assert.notNull(session_id, "Session ID cannot be null!");

        try
        {
            NegotiationSocketIO sess = new NegotiationSocketIO(world_id, handler.GetAgentID(), session_id);

            //<editor-fold defaultstate="collapsed" desc="Set On Sync Handler">
            sess.setOnSyncHandler(message -> {
                JSONNegotiationSession json = gson.fromJson(message, JSONNegotiationSession.class);
                handler.OnReceiveState(new State(json));

                active_state = json.state;

                switch (json.state)
                {
                    case "run":
                        logger.info(handler.GetAgentID() + " : session=" + session_id + " : state=run : turn=" + json.turn );
                        if (json.turn.equals("agent:"+ handler.GetAgentID()))
                        {
                            logger.info(handler.GetAgentID() + " : sess ");
                            if (_didBid < json.turn_count) break;
                            _didBid = json.turn_count;

                            edu.ozu.mapp.utils.Action action = handler.OnMakeAction();
                            action.bid.apply(this); // TODO change behaviour

                            sess.emitTakeAction(action.toString());
                        }
                        break;
                    case "done":
                        logger.info("negotiation session is done");

                        if (didDone) break;

                        handler.AcceptLastBids(json);

                        //<editor-fold defaultstate="collapsed" desc="close socket when negotiation done">
                        try { sess.close(); }
                        catch (IOException e) { e.printStackTrace(); }
                        //</editor-fold>

                        handler.PostNegotiation();

                        didDone = true;

                        logger.info(handler.GetAgentID() + " : session=" + session_id + " : state=done" );
                        break;
                }
            });
            //</editor-fold>
            //<editor-fold defaultstate="collapsed" desc="Set Will Join">
            sess.setWillJoinHandler(message -> {
                if (didJoin) return;
                logger.debug(message);

                handler.PrepareContract(this);

                sess.emitReady();
                didJoin = true;
                logger.info(handler.GetAgentID() + " : session=" + session_id + " : will join" );
            });
            //</editor-fold>
            //<editor-fold defaultstate="collapsed" desc="Set On Joined Handler">
            sess.setOnJoinedHandler(message -> {
                logger.debug(message);

                handler.PreNegotiation(session_id);

                logger.info(handler.GetAgentID() + " : session=" + session_id + " : joined" );
            });
            //</editor-fold>
            //<editor-fold defaultstate="collapsed" desc="Set On Joined Handler">
            sess.setOnNegotiationDoneHandler(message -> {
                // This should be invoked only once
                logger.debug(message);

                logger.info(handler.GetAgentID() + " : session=" + session_id + " : done");
            });
            //</editor-fold>

            sess.start();
        }
        catch (Exception exception) { exception.printStackTrace(); }
    }
    */
    //</editor-fold>

    void connectWS()
    {
        didBid = false;
        didJoin = false;
        active_state = "";
        bidding_agent = "";

        Assert.notNull(session_id, "Session ID cannot be null!");

        try {
            //!!! sessions contain only one session id for now
            String ws = "ws://" + Globals.SERVER + "/negotiation/" + world_id + "/" + session_id + "/" + handler.GetAgentID();
            NegotiationWS websocket = new NegotiationWS(new URI(ws));
//             * add handler
//             * Message format:
//             *  agent_count: <integer>                      | number of agents
//             *  bid_order: [agent_0, agent_1, ..., agent_i] | list of agent IDs.
//             *  bids     : [bid_agent_0, ..., bid_agent_i]  | list of bids of agents with IDs given
//             *  state    : {join|run|done}                  | state of the negotiation session
//             *  turn     : "agent_id"                       | ID of agent who's turn it is to bid
//             *
            websocket.setHandler(message -> {
                JSONNegotiationSession json = gson.fromJson(message, JSONNegotiationSession.class);
                handler.OnReceiveState(new State(json));                // pass session data to agent -> onReceiveState

                active_state = json.state;

                switch (json.state)
                {
                    case "join":
                        if (didJoin) break;

                        handler.PrepareContract(this);

                        logger.info("joining to negotiation session " + session_id);
                        // join negotiation session WS
                        // send ready message to socket
                        handler.PreNegotiation(session_id, new State(json));
                        handler.LogPreNegotiation(session_id);

                        logger.info("prepared for session " + session_id);
                        websocket.sendMessage("agent:" + handler.GetAgentID() + "-ready");
                        didJoin = true;

                        break;
                    case "run":
                        logger.info(handler.GetAgentID() + " : session=" + session_id + " : state=run : turn=" + json.turn );

                        if (json.turn.equals("agent:" + handler.GetAgentID()))
                        {   // own turn to bid
                            if (didBid) break;
                            didBid = true;  // don't make me do this again...

                            // bidding agent is empty in the first bid
                            if (!bidding_agent.isEmpty())
                            {   // no need to log it if first bid does not exist yet
                                handler.LogNegotiationState(bidding_agent);
                            }

                            edu.ozu.mapp.utils.Action action = handler.OnMakeAction();
                            logger.debug(action.toString());
                            action.bid.apply(this); //TODO change behaviour
                            websocket.sendMessage(action.toWSMSGString());

                            // bidding agent is me, and i made my bid
                            bidding_agent = json.turn;
                            handler.LogNegotiationState(bidding_agent, action);
                        } else {
                            bidding_agent = json.turn;
                            didBid = false;
                        }
                        break;
                    case "done":
                        logger.info("negotiation session is done");
                        logger.info("accepted -> " + message.trim());
                        handler.AcceptLastBids(json);
                        //<editor-fold defaultstate="collapsed" desc="close socket when negotiation done">
                        try {
                            websocket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        //</editor-fold>
                        handler.PostNegotiation();
                        handler.LogNegotiationOver(bidding_agent, session_id);
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
