package edu.ozu.drone.utils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Globals
{
    public static final int BROADCAST_SIZE = 3;

    public enum WorldState {
        JOIN("JOIN"),
        BROADCAST("BROADCAST"),
        NEGOTIATE("NEGOTIATE"),
        MOVE("MOVE");

        String value;

        WorldState(String value)
        {
            this.value = value.toUpperCase();
        }

        @Override
        public String toString()
        {
            return value;
        }
    }

    public static final Map<Integer, WorldState> WORLD_STATES = Collections.unmodifiableMap(
            new HashMap<Integer, WorldState>() {
                {
                    put(0, WorldState.JOIN);
                    put(1, WorldState.BROADCAST);
                    put(2, WorldState.NEGOTIATE);
                    put(3, WorldState.MOVE);
                }
            }
    );
}
