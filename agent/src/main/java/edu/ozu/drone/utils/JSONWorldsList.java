package edu.ozu.drone.utils;

import java.util.Arrays;

public class JSONWorldsList {
    private String[] worlds;

    public String[] getWorlds() { return worlds; }

    public void setWorlds(String[] worlds) { this.worlds = worlds; }

    @Override
    public String toString() {
        return "JSONWorldsList{" +
                "worlds=" + Arrays.toString(worlds) +
                '}';
    }
}
