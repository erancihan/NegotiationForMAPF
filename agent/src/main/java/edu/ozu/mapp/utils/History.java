package edu.ozu.mapp.utils;

import edu.ozu.mapp.agent.client.models.Contract;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

public class History
{
    private String agent_id;
    private String current_negotiation_id;

    public ConcurrentHashMap<String, ConcurrentLinkedDeque<Contract>> history;

    public History(String agent_id)
    {
        this.agent_id = agent_id;

        history = new ConcurrentHashMap<>();
    }

    public void setCurrentNegotiationID(String current_negotiation_id) {
        this.current_negotiation_id = current_negotiation_id;
    }

    public String getCurrentNegotiationID() {
        return current_negotiation_id;
    }

    public synchronized HashSet<Contract> get(String agent_id)
    {   // return bid history of AGENT_ID as HashSet
        HashSet<Contract> agent_history = new HashSet<>();

        if (current_negotiation_id == null) {
            return agent_history;
        }
        if (!history.containsKey(current_negotiation_id)) {
            history.put(current_negotiation_id, new ConcurrentLinkedDeque<>());
        }

        Iterator<Contract> iterator = history.get(current_negotiation_id).iterator();
        while (iterator.hasNext())
        {
            Contract contract = iterator.next();
            if (!contract.x.equals(agent_id)) continue;
            agent_history.add(contract);
        }

        return agent_history;
    }

    public void put(Contract contract)
    {
        put(this.agent_id, contract);
    }

    public void put(String agent_id, Contract contract)
    {
        if (current_negotiation_id == null) {
            // cannot put data in if not in negotiation
            return;
        }
        if (!history.containsKey(current_negotiation_id)) {
            history.put(current_negotiation_id, new ConcurrentLinkedDeque<>());
        }
        if (Objects.equals(history.get(current_negotiation_id).peekLast(), contract))
        {
            // last entry for current negotiation's history is the same
            return;
        }

        // Contract is different, save it
        try {
            history.get(current_negotiation_id).add(contract.clone());
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
    }

    public Contract last()
    {
        if (current_negotiation_id == null) {
            return null;
        }

        return history.get(current_negotiation_id).peekLast();
    }

    public Contract GetLastBid()
    {
        return last();
    }

    public synchronized Contract GetLastAgentBid(String id)
    {
        if (current_negotiation_id == null) {
            return null;
        }

        Contract contract = null;
        Iterator<Contract> iterator = history.get(current_negotiation_id).iterator();
        while (iterator.hasNext())
        {
            Contract next = iterator.next();
            if (next.x.replace("agent:", "").equals(id)) {
                try { contract = next.clone(); }
                catch (CloneNotSupportedException e) { e.printStackTrace(); System.exit(1); }
            }
        }

        return contract;
    }

    public Contract GetLastOwnBid()
    {
        return GetLastAgentBid(agent_id);
    }

    public Contract GetLastOpponentBid(String opponent_id)
    {
        return GetLastAgentBid(opponent_id);
    }

    public static void main(String[] args) {
        ConcurrentLinkedDeque<String> queue = new ConcurrentLinkedDeque<>();
        queue.add("0");
        queue.add("1");

        System.out.println(queue.peekLast());
    }
}
