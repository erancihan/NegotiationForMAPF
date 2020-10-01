package edu.ozu.mapp.utils;

public class JSONAgentData {
    public int id;
    public String agent_name;
    public String agent_class_name;
    public JSONPointData start;
    public JSONPointData dest;

    public JSONAgentData() {
    }

    public JSONAgentData(int id, String agent_name, String agent_class_name, Point start, Point dest) {
        this.id = id;
        this.agent_name = agent_name;
        this.agent_class_name = agent_class_name;
        this.start = new JSONPointData(start);
        this.dest = new JSONPointData(dest);
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
