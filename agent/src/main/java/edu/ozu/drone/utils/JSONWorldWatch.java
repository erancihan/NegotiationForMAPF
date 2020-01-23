package edu.ozu.drone.utils;

import com.google.gson.annotations.SerializedName;

import java.util.Arrays;

public class JSONWorldWatch {
    public String agent_id;
    public String world_id;
    @SerializedName("pc") public int player_count;
    @SerializedName("time") public long time;
    public int world_state;
    public String position;
    public String[][] fov;
    public int fov_size;
    public float exec_time;

    @Override
    public String toString() {
        return "JSONWorldWatch{" +
                "agent_id='" + agent_id + '\'' +
                ", world_id='" + world_id + '\'' +
                ", player_count=" + player_count +
                ", time=" + time +
                ", world_state=" + world_state +
                ", position='" + position + '\'' +
                ", fov=" + Arrays.deepToString(fov) +
                ", fov_size=" + fov_size +
                ", exec_time=" + exec_time +
                '}';
    }
}
