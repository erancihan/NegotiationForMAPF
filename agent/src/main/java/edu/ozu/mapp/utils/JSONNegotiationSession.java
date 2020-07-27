package edu.ozu.mapp.utils;

import com.google.gson.annotations.SerializedName;

import java.util.Arrays;

public class JSONNegotiationSession {
    @SerializedName("agent_count") public int agent_count;
    @SerializedName("bid_order") public String bid_order;
    @SerializedName("bids") public String[][] bids;
    @SerializedName("state") public String state;
    @SerializedName("turn") public String turn;
    @SerializedName("turn_count") public int turn_count;

    @Override
    public String toString() {
        return "JSONNegotiationSession{" +
                "agent_count=" + agent_count +
                ", bid_order='" + bid_order + '\'' +
                ", bids=" + Arrays.deepToString(bids) +
                ", state='" + state + '\'' +
                ", turn='" + turn + '\'' +
                '}';
    }
}
