package edu.ozu.mapp.utils;

public class State {
    int time_tick;
    public String[][] bids;
    String turn;

    public State(JSONNegotiationSession json)
    {
        this(json, 0);
    }

    public State(JSONNegotiationSession json, int time_tick)
    {
        this.time_tick = time_tick;

        bids = json.bids;
        turn = json.turn;
    }
}
