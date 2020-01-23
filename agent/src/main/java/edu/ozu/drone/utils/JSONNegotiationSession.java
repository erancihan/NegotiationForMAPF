package edu.ozu.drone.utils;

import com.google.gson.annotations.SerializedName;

import java.util.Arrays;

public class JSONNegotiationSession {
    @SerializedName("agent_count") public int agent_count;
    @SerializedName("bid_order") public String bid_order;
    @SerializedName("bids") public String[][] bids;
    @SerializedName("state") public String state;
    @SerializedName("turn") public String turn;

    @Override
    public String toString() {
        return "JSONNegotiationSession{" +
                "agent_count=" + agent_count +
                ", bid_order='" + bid_order + '\'' +
                ", bids=" + Arrays.toString(bids) +
                ", state='" + state + '\'' +
                ", turn='" + turn + '\'' +
                '}';
    }
}
