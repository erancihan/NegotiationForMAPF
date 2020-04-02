package edu.ozu.mapp.agent.client.models;

import edu.ozu.mapp.agent.Agent;
import edu.ozu.mapp.agent.client.NegotiationSession;
import edu.ozu.mapp.agent.client.handlers.JedisConnection;
import edu.ozu.mapp.keys.KeyHandler;

import java.util.Map;

public class Contract {
    public String Ox;
    public String x;
    private String ETa;
    public String a; // id of agent A
    private String ETb;
    public String b; // id of agent B
    private String sess_id;

    public Contract(Map<String, String> sess) {
        Ox = sess.getOrDefault("Ox", "");
        x = sess.getOrDefault("x", "");
        ETa = sess.getOrDefault("ETa", "");
        ETb = sess.getOrDefault("ETb", "");

        sess_id = sess.get("_session_id");
    }

    public String getETa(String AgentID) {
        return KeyHandler.decrypt(ETa, KeyHandler.getPubKey(AgentID));
    }

    public String getETb(String AgentID) {
        return KeyHandler.decrypt(ETb, KeyHandler.getPubKey(AgentID));
    }

    public String getToken(String AgentID) {
        if (a.equals(AgentID)) {
            return getETa(AgentID);
        }
        if (b.equals(AgentID)) {
            return getETb(AgentID);
        }

        return "";
    }

    public void set(Agent agent, String O, int next) {
        if (a.equals(agent.AGENT_ID))
        {
            int current = Integer.parseInt(getETa(a));
            if (current < next)
            {
                Ox = O;
                x = agent.AGENT_ID;
                ETa = KeyHandler.encrypt(String.valueOf(next), agent.keys.getPrivateKey(agent));
            }
        }

        if (b.equals(agent.AGENT_ID))
        {
            int current = Integer.parseInt(getETb(b));
            if (current < next)
            {
                Ox = O;
                x = agent.AGENT_ID;
                ETb = KeyHandler.encrypt(String.valueOf(next), agent.keys.getPrivateKey(agent));
            }
        }
    }

    public void apply(NegotiationSession session) {
        if (session.getActiveState().equals("run")) {
            redis.clients.jedis.Jedis jedis = JedisConnection.getInstance();

            jedis.hset("negotiation:"+sess_id, "Ox", Ox);
            jedis.hset("negotiation:"+sess_id, "x", x);
            jedis.hset("negotiation:"+sess_id, "ETa", ETa);
            jedis.hset("negotiation:"+sess_id, "ETb", ETb);
        }
    }
}
