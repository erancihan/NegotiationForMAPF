package edu.ozu.mapp.system;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DATA_LOG_DISPLAY implements Cloneable
{
    public Map<String, String> world_data;
    public Map<String, String> agent_to_point;
    public Map<String, Integer> agent_bank_info;
    public ArrayList<Object[]> world_log;
    public Map<String, ArrayList<String>> negotiation_logs;

    public DATA_LOG_DISPLAY()
    {
        world_data          = new HashMap<>();
        agent_to_point      = new HashMap<>();
        agent_bank_info     = new HashMap<>();
        world_log           = new ArrayList<>();
        negotiation_logs    = new HashMap<>();
    }

    public void LogAgentLocations(ConcurrentHashMap<String, String> agent_to_point)
    {
        this.agent_to_point.clear();
        this.agent_to_point.putAll(agent_to_point);
    }

    public void LogWorldTime(int time)
    {
        world_data.put("World TIME", String.valueOf(time));
    }

    public void LogAgentBankInfo(ConcurrentHashMap<String, Integer> bank_data)
    {
        this.agent_bank_info.clear();
        this.agent_bank_info.putAll(bank_data);
    }

    @Override
    protected DATA_LOG_DISPLAY clone() throws CloneNotSupportedException
    {
        return (DATA_LOG_DISPLAY) super.clone();
    }
}
