package edu.ozu.drone.utils;

import com.google.gson.annotations.SerializedName;

public class JSONNegotiationSession {
    @SerializedName("agent_count") public int agent_count;
    @SerializedName("bid_order") public String bid_order;
    @SerializedName("bids") public String[][] bids;
    @SerializedName("state") public String state;
    @SerializedName("turn") public String turn;
}
