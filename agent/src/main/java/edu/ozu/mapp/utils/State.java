package edu.ozu.mapp.utils;

import edu.ozu.mapp.agent.client.models.Contract;

import java.util.Arrays;

public class State {
    public int time_tick;
    public String[][] bids;
    public String turn;
    public String[] agents;
    public Contract contract;
    public String session_id;

    public State()
    {

    }

    public State(JSONNegotiationSession json)
    {
        this(json, 0);
    }

    public State(JSONNegotiationSession json, int time_tick)
    {
        this.time_tick = time_tick;

        bids = json.bids;
        turn = json.turn;
        agents = Arrays.stream(json.agents.split(",")).map(s -> s.replace("agent:", "")).toArray(String[]::new);
    }

    public String getTurn() {
        return turn;
    }

    @Override
    public String toString() {
        return "State{" +
                "time_tick=" + time_tick +
                ", bids=" + Arrays.toString(bids) +
                ", turn='" + turn + '\'' +
                ", agents=" + Arrays.toString(agents) +
                ", contract=" + contract +
                ", session_id='" + session_id + '\'' +
                '}';
    }
}
