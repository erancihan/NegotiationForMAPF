package edu.ozu.mapp.system;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class DATA_LOG_DISPLAY
{
    public Map<String, String> world_data;
    public ArrayList<Object[]> world_log;
    public Map<String, ArrayList<String>> negotiation_logs;

    public DATA_LOG_DISPLAY()
    {
        world_data          = new HashMap<>();
        world_log           = new ArrayList<>();
        negotiation_logs    = new HashMap<>();
    }
}
