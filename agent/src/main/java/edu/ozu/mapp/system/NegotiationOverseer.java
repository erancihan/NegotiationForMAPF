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
    private static NegotiationOverseer instance;

    protected BiConsumer<String, Integer> bank_update_hook;
    protected Consumer<String> world_log_callback;
    protected BiConsumer<String, String> log_payload_hook;

    // { AGENT_NAME: [ SESSION_HASH, ... ] }
    private ConcurrentHashMap<String, ArrayList<String>> session_keys;
    // { SESSION_HASH : [ NEGOTIATION_SESSION , ... ] }
    private ConcurrentHashMap<String, NegotiationSession> sessions;

    private int cumulative_negotiation_count = 0;

    private NegotiationOverseer()
    {
        session_keys = new ConcurrentHashMap<>();
        sessions     = new ConcurrentHashMap<>();
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

    public void Flush()
    {
        instance = new NegotiationOverseer();
    }

    public synchronized void RegisterCollisionNotification(String[] agent_ids)
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

        for (String agent_name : agent_ids)
        {
            ArrayList<String> sessions = session_keys.getOrDefault(agent_name, new ArrayList<>());

            if (sessions.contains(session_hash)) continue;

            sessions.add(session_hash);

            session_keys.put(agent_name, sessions);
        }

        if (!sessions.containsKey(session_hash))
        {
            sessions.put(session_hash, new NegotiationSession(session_hash, agent_ids, bank_update_hook, world_log_callback, log_payload_hook));
            cumulative_negotiation_count++;
        }
    }

    public String[] GetNegotiations(String agent_name)
    {
        return session_keys.getOrDefault(agent_name, new ArrayList<>()).toArray(new String[0]);
    }

    public void AgentJoinSession(String session_id, AgentHandler agent)
    {
        sessions.get(session_id).RegisterAgentREF(agent);

        world_log_callback.accept(
                String.format("%s joining %s | %s", agent.getAgentName(), session_id.substring(0, 7), Arrays.toString(sessions.get(session_id).GetAgentNames()))
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
    }

    public int CumulativeCount()
    {
        return cumulative_negotiation_count;
    }

    public int ActiveCount()
    {
        return sessions.keySet().size();
    }
}
