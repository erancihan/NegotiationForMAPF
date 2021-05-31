package edu.ozu.mapp.utils;

import com.google.gson.annotations.SerializedName;
import edu.ozu.mapp.system.FoV;

import java.util.Arrays;

public class JSONWorldWatch {
    public String agent_id;
    public String world_id;
    @SerializedName("pc") public int player_count;
    @SerializedName("time") public long time;
    public int world_state;
    public String position;
    public FoV fov;
    public int fov_size;
    public int time_tick;
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
                ", fov=" + fov.toString() +
                ", fov_size=" + fov_size +
                ", time_tick=" + time_tick +
                ", exec_time=" + exec_time +
                '}';
    }
}
