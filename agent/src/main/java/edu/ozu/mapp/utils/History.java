package edu.ozu.mapp.utils;

import edu.ozu.mapp.agent.client.models.Contract;

import java.util.*;

public class History
{
    private String agent_id;
    private String current_negotiation_id;

    public HashMap<String, List<Contract>> history;

    public History(String agent_id)
    {
        this.agent_id = agent_id;

        history = new HashMap<String, List<Contract>>();
    }

    public void setCurrentNegotiationID(String current_negotiation_id) {
        this.current_negotiation_id = current_negotiation_id;
    }

    public String getCurrentNegotiationID() {
        return current_negotiation_id;
    }

    public HashSet<Contract> get(String agent_id)
    {   // return bid history of AGENT_ID as HashSet
        HashSet<Contract> agent_history = new HashSet<>();

        if (current_negotiation_id == null) {
            return agent_history;
        }
        if (!history.containsKey(current_negotiation_id)) {
            history.put(current_negotiation_id, new ArrayList<Contract>());
        }

        for (Contract contract : history.get(current_negotiation_id))
        {
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
            history.put(current_negotiation_id, new ArrayList<Contract>());
        }
        if (
            history.get(current_negotiation_id).size() > 0 &&
            history.get(current_negotiation_id).get(history.get(current_negotiation_id).size() - 1).equals(contract)
        ) {
            // last entry for current negotiation's history is the same
            return;
        }

        // Contract is different, save it
        history.get(current_negotiation_id).add(contract);
    }

    public Contract last()
    {
        if (current_negotiation_id == null) {
            return null;
        }
        if (history.get(current_negotiation_id).size() == 0) {
            return null;
        }
        return history.get(current_negotiation_id).get(history.get(current_negotiation_id).size() - 1);
    }

    public Contract GetLastBid()
    {
        return last();
    }

    public Contract GetLastAgentBid(String id)
    {
        if (current_negotiation_id == null) {
            return null;
        }

        int current_history_size = history.get(current_negotiation_id).size();
        ListIterator<Contract> iterator = history.get(current_negotiation_id).listIterator(current_history_size);
        while (iterator.hasPrevious())
        {
            Contract contract = iterator.previous();
            if (contract.x.replace("agent:", "").equals(id)) {
                return contract;
            }
        }

        return null;
    }

    public Contract GetLastOwnBid()
    {
        return GetLastAgentBid(agent_id);
    }

    public Contract GetLastOpponentBid(String opponent_id)
    {
        return GetLastAgentBid(opponent_id);
    }
}
