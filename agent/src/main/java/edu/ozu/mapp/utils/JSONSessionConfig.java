package edu.ozu.mapp.utils;

import edu.ozu.mapp.config.AgentConfig;

import java.util.Arrays;

public class JSONSessionConfig {
    public AgentConfig[] agents;
    public JSONWorldData world;

    @Override
    public String toString() {
        return "\nJSONSessionConfig{\n agents=" + Arrays.toString(agents) + ",\n world=" + world + "\n}";
    }
}
