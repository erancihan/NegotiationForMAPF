package edu.ozu.drone.utils;

import com.google.gson.annotations.SerializedName;

public class JSONWorldCreate {
    @SerializedName("world_id")
    private String world_id;

    @SerializedName("player_count")
    private String player_count;

    @SerializedName("world_state")
    private String world_state;

    public String getWorld_id() { return world_id; }

    public void setWorld_id(String world_id) { this.world_id = world_id; }

    public String getPlayer_count() { return player_count; }

    public void setPlayer_count(String player_count) { this.player_count = player_count; }

    public String getWorld_state() { return world_state; }

    public void setWorld_state(String world_state) { this.world_state = world_state; }

    @Override
    public String toString() {
        return "JSONWorldCreate{" +
                "world_id='" + world_id + '\'' +
                ", player_count='" + player_count + '\'' +
                ", world_state='" + world_state + '\'' +
                '}';
    }
}
