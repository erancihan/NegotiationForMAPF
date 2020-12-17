package edu.ozu.mapp.system;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DATA_LOG_DISPLAY
{
    public Map<String, String> world_data;
    public Map<String, String> agent_to_point;
    public ArrayList<Object[]> world_log;
    public Map<String, ArrayList<String>> negotiation_logs;

    public DATA_LOG_DISPLAY()
    {
        world_data          = new HashMap<>();
        agent_to_point      = new HashMap<>();
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
    }
}
