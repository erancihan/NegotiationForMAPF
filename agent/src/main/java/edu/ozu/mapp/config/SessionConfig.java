package edu.ozu.mapp.config;

import java.util.Arrays;

public class SessionConfig {
    public AgentConfig[] agents;
    public WorldConfig world;

    @Override
    public String toString() {
        return "\nJSONSessionConfig{\n agents=" + Arrays.toString(agents) + ",\n world=" + world + "\n}";
    }
}
