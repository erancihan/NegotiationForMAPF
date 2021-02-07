package edu.ozu.mapp.config;

import com.google.gson.annotations.SerializedName;
import org.springframework.util.Assert;

public class WorldConfig {
    public String world_id;
    public int width;
    public int height;
    @SerializedName("min_allowed_path_length")
    public int min_path_len = 0;
    @SerializedName("max_allowed_path_length")
    public int max_path_len = 0;
    public int min_distance_between_agents;
    public int agent_count = 0;
    public int initial_token_c = 5;
    public Object[][] table_data;
    public int number_of_expected_conflicts;

    public WorldConfig() {
    }

    public WorldConfig(String world_id, int width, int height, int min_path_len, int min_distance_between_agents) {
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
                ", max_path_len=" + max_path_len +
                ", min_distance_between_agents=" + min_distance_between_agents +
                '}';
    }

    public void validate() {
        int c = 0;

        for (Object[] row : table_data) {
            c = c + (int) ((row[1] instanceof Integer) ? row[1] : Integer.parseInt(String.valueOf(row[1])));
        }

        if (agent_count == 0) {
            Assert.isTrue(agent_count == c, "Agent Count is not set correctly!");
        } else {
            agent_count = c;
        }
    }
}
