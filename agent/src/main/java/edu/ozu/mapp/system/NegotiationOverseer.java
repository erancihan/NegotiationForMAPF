package edu.ozu.mapp.system;

import edu.ozu.mapp.agent.Agent;
import edu.ozu.mapp.agent.client.AgentHandler;
import edu.ozu.mapp.agent.client.models.Contract;

import javax.xml.bind.DatatypeConverter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class NegotiationOverseer
{
    private static org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(NegotiationOverseer.class);

    private static NegotiationOverseer instance;

    protected BiConsumer<String, Integer> bank_update_hook;
    protected Consumer<String> world_log_callback;
    protected BiConsumer<String, String> log_payload_hook;

    // { AGENT_NAME: [ SESSION_HASH, ... ] }
    private ConcurrentHashMap<String, ArrayList<String>> session_keys;
    // { SESSION_HASH : [ NEGOTIATION_SESSION , ... ] }
    private ConcurrentHashMap<String, NegotiationSession> sessions;

    private int cumulative_negotiation_count;

    private NegotiationOverseer()
    {
        session_keys = new ConcurrentHashMap<>();
        sessions     = new ConcurrentHashMap<>();

        cumulative_negotiation_count = 0;
    }

    public static NegotiationOverseer getInstance()
    {
        if (instance == null)
        {
            synchronized (NegotiationOverseer.class)
            {
                if (instance == null)
                {
                    instance = new NegotiationOverseer();
                }
            }
        }

        return instance;
    }

    public NegotiationOverseer Flush()
    {
        instance = new NegotiationOverseer();

        return instance;
    }

    public synchronized String RegisterCollisionNotification(String[] agent_ids)
    {
        // sort data first
        Arrays.sort(agent_ids);

        // PREPARE SESSION HASH
        StringBuilder session_key = new StringBuilder();
        for (String id : agent_ids)
        {
            session_key.append(id);
        }

        String session_hash = session_key.toString();
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            byte[] digest = md5.digest(session_key.toString().getBytes(StandardCharsets.UTF_8));
            session_hash = DatatypeConverter.printHexBinary(digest);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        boolean is_ok = true;
        for (String agent_name : agent_ids)
        {
            ArrayList<String> sessions = session_keys.getOrDefault(agent_name, new ArrayList<>());

            is_ok = is_ok && sessions.isEmpty();
        }

        if (is_ok)
        {
            for (String agent_id : agent_ids)
            {
                ArrayList<String> sessions = session_keys.getOrDefault(agent_id, new ArrayList<>());
                sessions.add(session_hash);
                session_keys.put(agent_id, sessions);
            }
        }

        if (!sessions.containsKey(session_hash))
        {
            sessions.put(session_hash, new NegotiationSession(session_hash, agent_ids, bank_update_hook, world_log_callback, log_payload_hook));
            cumulative_negotiation_count++;
        }

        return session_hash;
    }

    public String[] GetNegotiations(String agent_name)
    {
        return session_keys.getOrDefault(agent_name, new ArrayList<>()).toArray(new String[0]);
    }

    public void AgentJoinSession(String session_hash, AgentHandler agent)
    {
        // get session members
        String[] agents = sessions.get(session_hash).GetAgentIDs();
        if (!Arrays.asList(agents).contains(agent.GetAgentID()))
        {
            logger.error(agent.GetAgentID() + " does not belong to session " + session_hash + " | " + Arrays.toString(agents));
            System.exit(1);
        }

        // are these ids present in session info
        for (String agent_id : agents)
        {
            if (agent_id.equals(agent.GetAgentID()))
            {   // this is me, i am already joining.
                // the problem might be with my partners
                continue;
            }

            ArrayList<String> keys = session_keys.getOrDefault(agent_id, new ArrayList<>());

            if (!keys.contains(session_hash))
            {   // i am supposed to be in this negotiation
                if (keys.size() > 0)
                {   // oh wait there is another negotiation i am supposed to attend
                    continue;
                }

                logger.warn(agent_id + " | for some reason I am not in session " + session_hash);
                keys.add(session_hash);
                session_keys.put(agent_id, keys);
                System.exit(1);
            }
        }

        sessions.get(session_hash).RegisterAgentREF(agent);

        world_log_callback.accept(
                String.format("%s joining %s | %s", agent.getAgentName(), session_hash.substring(0, 7), Arrays.toString(sessions.get(session_hash).GetAgentIDs()))
        );
    }

    public Contract GetMyContract(Agent agent)
    {
        String agent_name = agent.AGENT_NAME;

        // TODO retrieve from AGENT
        String session_key = session_keys.get(agent_name).get(0);

        return sessions.get(session_key).GetAgentContract(agent_name);
    }

    public synchronized void AgentLeaveSession(String agent_name, String session_id)
    {
        NegotiationSession session = sessions.get(session_id);

        if (session.GetState().equals(NegotiationSession.NegotiationState.DONE))
        {
            session.RegisterAgentLeaving(agent_name);
            session_keys.get(agent_name).remove(session_id);
        }
        if (session.GetActiveAgentNames().length == 0)
        {
//            session.destroy();
            sessions.remove(session_id);

            String timestamp = new java.sql.Timestamp(System.currentTimeMillis()).toString();
            log_payload_hook.accept(String.format("%s-%s",  session_id, timestamp), String.format("%-23s %s", timestamp, "DELETED"));
            world_log_callback.accept(String.format("Negotiation Session %s TERMINATED", session_id.substring(0, 7)));
        }

        logger.debug(agent_name + " leaving session " + session_id);
        logger.debug("remaining negotiation sessions " + sessions);
    }

    public int CumulativeCount()
    {
        return cumulative_negotiation_count;
    }

    public int ActiveCount()
    {
        return sessions.keySet().size();
    }

    public String[] GetSessionAgents(String session)
    {
        return sessions.get(session).GetAgentIDs();
    }

    public String[] InvalidateSession(String session) {
        String[] agent_names = sessions.get(session).GetAgentIDs();

        sessions.get(session).invalidate();
        sessions.remove(session);

        for (String key : agent_names)
            session_keys.getOrDefault(key, new ArrayList<>()).remove(session);

        return agent_names;
    }
}
