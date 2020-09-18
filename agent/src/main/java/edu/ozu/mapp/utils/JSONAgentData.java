package edu.ozu.mapp.utils;

public class JSONAgentData {
    public String agent_name;
    public String agent_class_name;
    public String start;
    public String dest;

    public JSONAgentData() {
    }

    public JSONAgentData(String agent_name, String agent_class_name, Point start, Point dest) {
        this.agent_name = agent_name;
        this.agent_class_name = agent_class_name;
        this.start = start.key;
        this.dest = dest.key;
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
