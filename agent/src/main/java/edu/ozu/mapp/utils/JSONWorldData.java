package edu.ozu.mapp.utils;

public class JSONWorldData {
    public String world_id;
    public int width;
    public int height;
    public int min_path_len;
    public int min_distance_between_agents;
    public int agent_count = 0;

    public JSONWorldData() {
    }

    public JSONWorldData(String world_id, int width, int height, int min_path_len, int min_distance_between_agents) {
        this.world_id = world_id;
        this.width = width;
        this.height = height;
        this.min_path_len = min_path_len;
        this.min_distance_between_agents = min_distance_between_agents;
    }

    @Override
    public String toString() {
        return "JSONWorldData{" +
                "wid='" + world_id + '\'' +
                ", width=" + width +
                ", height=" + height +
                ", min_path_len=" + min_path_len +
                ", min_distance_between_agents=" + min_distance_between_agents +
                '}';
    }
}
