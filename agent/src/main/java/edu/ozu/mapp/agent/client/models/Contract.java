package edu.ozu.mapp.agent.client.models;

import edu.ozu.mapp.agent.Agent;
import edu.ozu.mapp.utils.Point;
import org.springframework.util.Assert;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Contract implements Cloneable {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Contract.class);
    private final boolean HAS_ENCRYPTION = false;

    public String Ox;
    public String x;
    private String ETa = "0";
    public String A = ""; // id of agent A
    private String ETb = "0";
    public String B = ""; // id of agent B
    public String sess_id = "";

    private HashMap<String, String> offers;

    public Contract(Map<String, String> sess)
    {
        Ox = sess.getOrDefault("Ox", "");
        x = sess.getOrDefault("x", "");

        A = sess.get("A");
        B = sess.get("B");

        ETa = sess.getOrDefault("ETa", "0");
        ETb = sess.getOrDefault("ETb", "0");

        sess_id = sess.get("_session_id");
        Assert.isTrue(!sess_id.isEmpty(), "Session ID cannot be null");

        offers = new HashMap<>();
        offers.put(A, ETa);
        offers.put(B, ETb);
    }

    public int GetTokenCountOf(Agent agent)
    {
        return Integer.parseInt(getTokenCountOf(agent));
    }

    public String getTokenCountOf(Agent agent)
    {
//        System.out.println(A + " " + B + " " + agent.AGENT_ID);
        if (offers.containsKey(agent.AGENT_ID))
        {
//            return HAS_ENCRYPTION ? agent.Decrypt(offers.get(agent.AGENT_ID)) : offers.get(agent.AGENT_ID);
            return offers.get(agent.AGENT_ID);
        }
        System.err.println(agent.AGENT_ID + " : " + offers);
        return null;
    }

    public void set(Agent agent, String O, int next)
    {
        logger.debug("{A:"+A+", B:"+B+", Ox:"+O+", x:"+agent.AGENT_ID+"}");
        if (offers.containsKey(agent.AGENT_ID))
        {
            logger.debug("{current:"+offers.get(agent.AGENT_ID)+", next:"+next+"}");
//            if (current <= next)  // TODO take care of this later
            {
                Ox = O;
                x = agent.AGENT_ID;
                offers.put(
                        agent.AGENT_ID,
//                        HAS_ENCRYPTION ? agent.Encrypt(String.valueOf(next)) : String.valueOf(next)
                        String.valueOf(next)
                );
            }

            if (A.equals(agent.AGENT_ID))
            {
//                ETa = HAS_ENCRYPTION ? agent.Encrypt(String.valueOf(next)) : String.valueOf(next);
                ETa = String.valueOf(next);
            }
            if (B.equals(agent.AGENT_ID))
            {
//                ETb = HAS_ENCRYPTION ? agent.Encrypt(String.valueOf(next)) : String.valueOf(next);
                ETb = String.valueOf(next);
            }
        }

        logger.debug("Contract@Set{A:"+A+", B:"+B+", Ox:"+Ox+", x:"+agent.AGENT_ID+"}");
        agent.OnContractUpdated(this);
    }

    public void apply(edu.ozu.mapp.system.NegotiationSession session)
    {
        logger.debug("apply:{state:"+session.GetState()+"}");
        if (
            session.GetState().equals(edu.ozu.mapp.system.NegotiationSession.NegotiationState.JOIN) &&
            session.GetState().equals(edu.ozu.mapp.system.NegotiationSession.NegotiationState.RUNNING)
        )
        {
            session.SetContract(this);
        }
    }

    public boolean isAgentSet(Agent agent)
    {
        if (offers.containsKey(agent.AGENT_ID))
        {
            return !offers.get(agent.AGENT_ID).isEmpty();
        }

        return false;
    }

    public int GetOpponentTokenProposal(Agent agent)
    {
        if (offers.containsKey(agent.current_opponent))
        {
            // this should be bi-lateral
            assert offers.keySet().size() == 2;

            String opponent_key = null;
            for (String key : offers.keySet())
            {
                if (key.equals(agent.AGENT_ID)) continue;
                opponent_key = key;
            }

            assert opponent_key != null;

            return Integer.parseInt(offers.get(opponent_key));
        }
        return -1;
    }

    public Point[] GetOx()
    {
        return Arrays
                .stream(Ox.replaceAll("([\\[\\]]*)", "").split(","))
                .map(item -> item.isEmpty() ? null : new Point(item.trim(), "-"))
                .toArray(Point[]::new)
                ;
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
            "{A: %s, ETa: %s, B: %s, ETb: %s, x: %s, Ox: %s}",
            A, ETa, B, ETb, x, Ox
        );
    }
}
