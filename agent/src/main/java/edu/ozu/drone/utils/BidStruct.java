package edu.ozu.drone.utils;

public class BidStruct {
    public String agent_id;
    public String path;
    public int token_count;

    public BidStruct(String agent_id, String path, int token_count) {
        this.agent_id = agent_id;
        this.path = path;
        this.token_count = token_count;
    }
}
