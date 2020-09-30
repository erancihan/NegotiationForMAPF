package edu.ozu.mapp.utils;

import java.util.Arrays;

public class State {
    int time_tick;
    public String[][] bids;
    String turn;
    String[] agents;

    public State(JSONNegotiationSession json)
    {
        this(json, 0);
    }

    public State(JSONNegotiationSession json, int time_tick)
    {
        this.time_tick = time_tick;

        bids = json.bids;
        turn = json.turn;
        agents = Arrays.stream(json.agents.split(",")).map(s -> s.replace("agents:", "")).toArray(String[]::new);
    }

    public String getTurn() {
        return turn;
    }
}
