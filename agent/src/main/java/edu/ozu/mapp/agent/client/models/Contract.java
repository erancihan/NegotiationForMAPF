package edu.ozu.mapp.agent.client.models;

import edu.ozu.mapp.agent.Agent;
import edu.ozu.mapp.agent.client.AgentHandler;
import edu.ozu.mapp.utils.Globals;
import org.springframework.util.Assert;
import redis.clients.jedis.Jedis;

import java.util.Map;
import java.util.Objects;

public class Contract implements Cloneable {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Contract.class);
    private static final boolean HAS_ENCRYPTION = false;

    public String Ox;
    public String x;
    private String ETa = "0";
    public String A = ""; // id of agent A
    private String ETb = "0";
    public String B = ""; // id of agent B
    private String sess_id = "";
    private boolean is_serverless = false;

    private Contract(Map<String, String> sess)
    {
        Ox = sess.getOrDefault("Ox", "");
        x = sess.getOrDefault("x", "");

        A = sess.get("A");
        B = sess.get("B");

        ETa = sess.getOrDefault("ETa", "0");
        ETb = sess.getOrDefault("ETb", "0");

        sess_id = sess.get("_session_id");
        Assert.isTrue(!sess_id.isEmpty(), "Session ID cannot be null");
    }

    public static Contract Create(Map<String, String> sess)
    {
//        Assert.isTrue(!sess.get("x").isEmpty(), "<<Contract cannot be empty>>");
//        if (sess == null || sess.getOrDefault("x", "").isEmpty())
//            return null;

        return new Contract(sess);
    }

    public String getETa(Agent agent)
    {
        return HAS_ENCRYPTION ? agent.Decrypt(ETa) : ETa;
    }

    public String getETb(Agent agent)
    {
        return HAS_ENCRYPTION ? agent.Decrypt(ETb) : ETb;
    }

    public String getTokenCountOf(Agent agent)
    {
        System.out.println(A + " " + B + " " + agent.AGENT_ID);
        if (A.equals(agent.AGENT_ID)) {
            return getETa(agent);
        }
        if (B.equals(agent.AGENT_ID)) {
            return getETb(agent);
        }

        return null;
    }

    public void set(Agent agent, String O, int next)
    {
        logger.debug("{A:"+A+", B:"+B+", Ox:"+O+", x:"+agent.AGENT_ID+"}");
        if (A.equals(agent.AGENT_ID))
        {
            int current = Integer.parseInt(getETa(agent));
            logger.debug("{current:"+current+", next:"+next+"}");
//            if (current <= next)  // TODO take care of this later
            {
                Ox = O;
                x = agent.AGENT_ID;
                ETa = HAS_ENCRYPTION ? agent.Encrypt(String.valueOf(next)) : String.valueOf(next);
            }
        }

        if (B.equals(agent.AGENT_ID))
        {
            int current = Integer.parseInt(getETb(agent));
            logger.debug("{current:"+current+", next:"+next+"}");
//            if (current <= next)  // TODO take care of this later
            {
                Ox = O;
                x = agent.AGENT_ID;
                ETb = HAS_ENCRYPTION ? agent.Encrypt(String.valueOf(next)) : String.valueOf(next);
            }
        }
        logger.debug("Contract@Set{A:"+A+", B:"+B+", Ox:"+Ox+", x:"+agent.AGENT_ID+"}");
        agent.OnContractUpdated(this);
    }

    private void apply()
    {
        logger.debug("apply:{Ox:"+Ox+", session_id:"+sess_id+"}");
        if (is_serverless) return;

        redis.clients.jedis.Jedis jedis = new Jedis(Globals.REDIS_HOST, Globals.REDIS_PORT);

        jedis.hset("negotiation:"+sess_id, "Ox", Ox);
        jedis.hset("negotiation:"+sess_id, "x", x);
        jedis.hset("negotiation:"+sess_id, "ETa", ETa);
        jedis.hset("negotiation:"+sess_id, "A", A);
        jedis.hset("negotiation:"+sess_id, "ETb", ETb);
        jedis.hset("negotiation:"+sess_id, "B", B);

        jedis.close();
    }

    public void apply(edu.ozu.mapp.system.NegotiationSession session)
    {
        is_serverless = true;

        logger.debug("apply:{state:"+session.GetState()+"}");
        if (
            session.GetState().equals(edu.ozu.mapp.system.NegotiationSession.NegotiationState.JOIN) &&
            session.GetState().equals(edu.ozu.mapp.system.NegotiationSession.NegotiationState.RUNNING)
        )
        {
            apply();
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

    @Override
    public String toString()
    {
        return String.format("Contract{Ox:'%s', x:'%s', ETa:'%s', A:'%s', ETb:'%s', B:'%s', sess_id:'%s'}", Ox, x, ETa, A, ETb, B, sess_id);
    }

    public Object getJSON() {
        return String.format("{\"Ox\":\"%s\", \"x\":\"%s\", \"ETa\":\"%s\", \"A\":\"%s\", \"ETb\":\"%s\", \"B\":\"%s\", \"sess_id\":\"%s\"}", Ox, x, ETa, A, ETb, B, sess_id);
    }

    @Override
    public Contract clone() throws CloneNotSupportedException {
        return (Contract) super.clone();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Contract)) return false;
        Contract contract = (Contract) o;
        return Objects.equals(Ox, contract.Ox) &&
                Objects.equals(x, contract.x) &&
                Objects.equals(ETa, contract.ETa) &&
                Objects.equals(A, contract.A) &&
                Objects.equals(ETb, contract.ETb) &&
                Objects.equals(B, contract.B) &&
                Objects.equals(sess_id, contract.sess_id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(Ox, x, ETa, A, ETb, B, sess_id);
    }

    public String print()
    {
        return String.format(
            "{SESS_ID: %s, A: %s, ETa: %s, B: %s, ETb: %s, x: %s, Ox: %s}",
            sess_id.substring(0, 7), A, ETa, B, ETb, x, Ox
        );
    }
}
