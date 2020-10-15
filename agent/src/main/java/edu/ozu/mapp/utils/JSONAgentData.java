package edu.ozu.mapp.utils;

public class JSONAgentData {
    public int id;
    public String agent_name;
    public String agent_class_name;
    public JSONPointData start;
    public JSONPointData dest;
    public int token_c = -1;        // un-initialized
    public int path_length = -1;    // un-initialized

    public JSONAgentData() {
    }
    public JSONAgentData(int id, String agent_name, String agent_class_name, Point start, Point dest) {
        this(id, agent_name, agent_class_name, Globals.INITIAL_TOKEN_BALANCE, start, dest);
    }

    public JSONAgentData(int id, String agent_name, String agent_class_name, int initial_token_count, Point start, Point dest) {
        this.id                     = id;
        this.agent_name             = agent_name;
        this.agent_class_name       = agent_class_name;
        this.token_c                = initial_token_count;
        this.start                  = new JSONPointData(start);
        this.dest                   = new JSONPointData(dest);
    }

    @Override
    public String toString() {
        return "JSONAgentData{" +
                "agent_name='" + agent_name + '\'' +
                ", agent_class_name='" + agent_class_name + '\'' +
                ", start='" + start + '\'' +
                ", dest='" + dest + '\'' +
                '}';
    }
}
