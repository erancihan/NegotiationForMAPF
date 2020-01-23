package edu.ozu.drone.utils;

import com.google.gson.annotations.SerializedName;

public class JSONAgent {
    @SerializedName("agent_id") public String agent_id;
    @SerializedName("agent_y") public String agent_x;
    @SerializedName("agent_x") public String agent_y;

    @Override
    public String toString() {
        return "JSONAgent{" +
                "agent_id='" + agent_id + '\'' +
                ", agent_x='" + agent_x + '\'' +
                ", agent_y='" + agent_y + '\'' +
                '}';
    }
}
