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

    public boolean equals(BidStruct o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        return token_count == o.token_count && agent_id.equals(o.agent_id) && path.equals(o.path);
    }

    @Override
    public String toString() {
        return "BidStruct{" +
                "agent_id='" + agent_id + '\'' +
                ", path='" + path + '\'' +
                ", token_count=" + token_count +
                '}';
    }
}
