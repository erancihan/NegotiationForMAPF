package edu.ozu.mapp.agent.client.models;

import edu.ozu.mapp.agent.Agent;
import edu.ozu.mapp.agent.client.NegotiationSession;
import edu.ozu.mapp.agent.client.helpers.JedisConnection;
import edu.ozu.mapp.keys.KeyHandler;
import org.springframework.util.Assert;
import redis.clients.jedis.Jedis;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Contract {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Contract.class);
    private static final boolean HAS_ENCRYPTION = false;

    public String Ox;
    public String x;
    private String ETa = "";
    public String A = ""; // id of agent A
    private String ETb = "";
    public String B = ""; // id of agent B
    private String sess_id = "";

    private Contract(Map<String, String> sess)
    {
        Ox = sess.getOrDefault("Ox", "");
        x = sess.getOrDefault("x", "");

        A = sess.get("A");
        B = sess.get("B");

        ETa = sess.getOrDefault("ETa", "");
        ETb = sess.getOrDefault("ETb", "");

        sess_id = sess.get("_session_id");
        Assert.isTrue(!sess_id.isEmpty(), "Session ID cannot be null");
    }

    public static Contract Create(Map<String, String> sess)
    {
//        Assert.isTrue(!sess.get("x").isEmpty(), "<<Contract cannot be empty>>");
        if (sess == null || sess.getOrDefault("x", "").isEmpty())
            return null;

        return new Contract(sess);
    }

    public static Contract Create(Agent agent, NegotiationSession session)
    {
        Jedis jedis = JedisConnection.getInstance();

        // Session ID is privately available in the
        // Negotiation Session class. Let's keep it that way
        String s_id = jedis.hget("world:"+agent.WORLD_ID+":notify", "agent:"+agent.AGENT_ID);
        if (s_id.split(",").length > 1)
        {   // This should be to ensure that agents only engage
            // in a single negotiation session. For now
            logger.error("something went wrong!");
            System.exit(1);
        }

        // There should be nothing in the REDIS at all
        Map<String, String> sess = new HashMap<>();
        sess = jedis.hgetAll("negotiation:"+s_id);
        sess.put("_session_id", s_id);

        // Decide on A|B if they are not already set (which they are not)
        // for agents. Since these are bilateral negotiations,
        // it is absolutely guaranteed that there will be no more
        // than 2 Agents in a Negotiation Session.
        // Negotiation Session data should be present, since "Notify"
        // flow have to be completed before agents establish
        // connection to Negotiation Session @ the back-end.
        // Fetch agents list and set them as A & B.
        // IDs are sorted & in "agents:<agent_id>" format.
        String[] agents = jedis.hget("negotiation:"+s_id, "agents").split(",");
        Assert.isTrue(agents.length == 2, "<<something went horribly wrong>>");

        // update agent list format
        for (int i = 0; i < agents.length; i++) { agents[i] = agents[i].split(":")[1]; }

        // set session values
        // Ox -> Bid of agent with id X
        sess.put("Ox", "");
        // x  -> ID of agents whose turn it is to bid
        String x = jedis.hget("negotiation:"+s_id, "bid_order").split(",")[0];
        sess.put("x", x);
        // ID of agent A
        sess.put("A", agents[0]);
        if (agent.AGENT_ID.equals(agents[0]))
        {   // set ETa, if A is the invoking agent
            if (HAS_ENCRYPTION) sess.put("ETa", KeyHandler.encrypt("0", agent));
            else sess.put("ETa", "0");
        }
        // ID of agent B
        sess.put("B", agents[1]);
        if (agent.AGENT_ID.equals(agents[1]))
        {   // set ETb, if B is the invoking agent
            if (HAS_ENCRYPTION) sess.put("ETb", KeyHandler.encrypt("0", agent));
            else sess.put("ETb", "0");
        }

        // finally, create contract for real this time
        // like actually create it
        // for realz... INITIATE IT
        Contract contract = new Contract(sess);
        contract.apply();

        logger.info("created contract for " + s_id + " with " + agent.AGENT_ID);

        return contract;
    }

    public String getETa(Agent agent)
    {
        return HAS_ENCRYPTION ? agent.Decrypt(ETa) : ETa;
    }

    public String getETb(Agent agent)
    {
        return HAS_ENCRYPTION ? agent.Decrypt(ETb) : ETb;
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
        logger.debug("Cotract@Set{A:"+A+", B:"+B+", Ox:"+Ox+", x:"+agent.AGENT_ID+"}");
        agent.OnContractUpdated(this);
    }

    private void apply()
    {
        logger.debug("apply:{Ox:"+Ox+", session_id:"+sess_id+"}");
        redis.clients.jedis.Jedis jedis = JedisConnection.getInstance();

        jedis.hset("negotiation:"+sess_id, "Ox", Ox);
        jedis.hset("negotiation:"+sess_id, "x", x);
        jedis.hset("negotiation:"+sess_id, "ETa", ETa);
        jedis.hset("negotiation:"+sess_id, "A", A);
        jedis.hset("negotiation:"+sess_id, "ETb", ETb);
        jedis.hset("negotiation:"+sess_id, "B", B);
    }

    public void apply(NegotiationSession session)
    {
        logger.debug("apply:{state:"+session.getActiveState()+"}");
        if (session.getActiveState().equals("run") || session.getActiveState().equals("join"))
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
}
