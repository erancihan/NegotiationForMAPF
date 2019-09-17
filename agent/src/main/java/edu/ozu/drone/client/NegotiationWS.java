package edu.ozu.drone.client;


import javax.websocket.*;
import java.io.IOException;

@ClientEndpoint
public class NegotiationWS {
    private Session session = null;
    private NegotiationWS.MessageHandler handler;

    public NegotiationWS(java.net.URI endpoint)
    {
        try
        {
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
        session.close();
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
