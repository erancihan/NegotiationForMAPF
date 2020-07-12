package edu.ozu.mapp.agent.client.models;

import edu.ozu.mapp.agent.Agent;
import edu.ozu.mapp.agent.client.NegotiationSession;
import edu.ozu.mapp.agent.client.handlers.JedisConnection;
import edu.ozu.mapp.keys.KeyHandler;

import java.util.Map;

public class Contract {
    public String Ox;
    public String x;
    private String ETa = "";
    public String A = ""; // id of agent A
    private String ETb = "";
    public String B = ""; // id of agent B
    private String sess_id = "";

    public Contract(Map<String, String> sess)
    {
        Ox = sess.getOrDefault("Ox", "");
        x = sess.getOrDefault("x", "");

        A = sess.get("A");
        B = sess.get("B");

        ETa = sess.getOrDefault("ETa", "");
        ETb = sess.getOrDefault("ETb", "");

        sess_id = sess.get("_session_id");
    }

    public String getETa(Agent agent)
    {
        return KeyHandler.decrypt(ETa, agent.GetPubKey());
    }

    public String getETb(Agent agent)
    {
        return KeyHandler.decrypt(ETb, agent.GetPubKey());
    }

    public String getTokenOf(Agent agent)
    {
        if (A.equals(agent.AGENT_ID)) {
            return getETa(agent);
        }
        if (B.equals(agent.AGENT_ID)) {
            return getETb(agent);
        }

        return "";
    }

    public void set(Agent agent, String O, int next)
    {
        if (A.equals(agent.AGENT_ID))
        {
            int current = Integer.parseInt(getETa(agent));
            if (current < next)
            {
                Ox = O;
                x = agent.AGENT_ID;
                ETa = KeyHandler.encrypt(String.valueOf(next), agent.keys.getPrivateKey(agent));
            }
        }

        if (B.equals(agent.AGENT_ID))
        {
            int current = Integer.parseInt(getETb(agent));
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
            jedis.hset("negotiation:"+sess_id, "A", A);
            jedis.hset("negotiation:"+sess_id, "ETb", ETb);
            jedis.hset("negotiation:"+sess_id, "B", B);
        }
    }

    public boolean isAgentSet(Agent agent) {
        if (A.equals(agent.AGENT_ID)) {
            return !ETa.isEmpty();
        }
        if (B.equals(agent.AGENT_ID)) {
            return !ETb.isEmpty();
        }

        return true;
    }
}
