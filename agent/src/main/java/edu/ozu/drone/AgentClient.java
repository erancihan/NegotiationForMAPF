package edu.ozu.drone;

public class AgentClient extends Runner implements Runnable {
    public AgentClient() { }

    @Override
    public void run() {
        name();
    }

    public String agentID() { return ""; }

    public void name() { }
}
