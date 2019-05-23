package edu.ozu.drone;

public class AgentClient extends Runner implements Runnable {
    private String WS_URL = "ws://";

    protected String AGENT_NAME = "";
    protected String AGENT_ID   = "";
    protected String START_X    = "";
    protected String START_Y    = "";

    public AgentClient() { }

    @Override
    public void run() {
        System.out.println(AGENT_NAME);
    }

    public void init() { }
}
