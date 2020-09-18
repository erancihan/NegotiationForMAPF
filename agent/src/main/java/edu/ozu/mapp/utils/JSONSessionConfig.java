package edu.ozu.mapp.utils;

import java.util.Arrays;

public class JSONSessionConfig {
    public JSONAgentData[] agents;
    public JSONWorldData world;

    @Override
    public String toString() {
        return "\nJSONSessionConfig{\n agents=" + Arrays.toString(agents) + ",\n world=" + world + "\n}";
    }
}
