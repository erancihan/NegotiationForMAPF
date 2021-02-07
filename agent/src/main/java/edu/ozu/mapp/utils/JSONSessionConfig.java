package edu.ozu.mapp.utils;

import edu.ozu.mapp.config.AgentConfig;
import edu.ozu.mapp.config.WorldConfig;

import java.util.Arrays;

public class JSONSessionConfig {
    public AgentConfig[] agents;
    public WorldConfig world;

    @Override
    public String toString() {
        return "\nJSONSessionConfig{\n agents=" + Arrays.toString(agents) + ",\n world=" + world + "\n}";
    }
}
