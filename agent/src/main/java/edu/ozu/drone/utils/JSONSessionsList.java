package edu.ozu.drone.utils;

import java.util.Arrays;

public class JSONSessionsList {
    private String[] sessions;

    public void setSessions(String[] sessions) {
        this.sessions = sessions;
    }

    public String[] getSessions() {
        return sessions;
    }

    @Override
    public String toString() {
        return "JSONSessionsList{" +
                "sessions=" + Arrays.toString(sessions) +
                '}';
    }
}
