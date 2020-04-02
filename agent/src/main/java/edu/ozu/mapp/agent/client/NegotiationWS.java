package edu.ozu.mapp.agent.client;


import javax.websocket.*;
import java.io.IOException;

@ClientEndpoint
public class NegotiationWS {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(NegotiationWS.class);
    private Session session = null;
    private NegotiationWS.MessageHandler handler;

    public NegotiationWS(java.net.URI endpoint)
    {
        try
        {
            logger.debug("creating new connection");
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            container.connectToServer(this, endpoint);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @OnOpen
    public void onOpen(Session session)
    {
        this.session = session;
    }

    @OnClose
    public void onClose(Session session, CloseReason reason)
    {
        this.session = null;
    }

    @OnMessage
    public void onMessage(String message)
    {
        if (handler != null)
            handler.handle(message);
    }

    public void close() throws IOException
    {
        logger.info("negotiation ws close");
        session.close();
        session = null;
    }

    public void setHandler(MessageHandler handler)
    {
        this.handler = handler;
    }

    public void sendMessage(String message)
    {
        this.session.getAsyncRemote().sendText(message);
    }

    public interface MessageHandler
    {
        void handle(String message);
    }
}
