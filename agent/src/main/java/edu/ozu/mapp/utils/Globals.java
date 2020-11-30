package edu.ozu.mapp.utils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Globals
{
    public static final int BROADCAST_SIZE = 5;
    public static final String SERVER = "localhost:3001";
    public static final String REDIS_HOST = "localhost";
    public static final int REDIS_PORT = 6379;
    public static final int INITIAL_TOKEN_BALANCE = 5;
    public static final int FIELD_OF_VIEW_SIZE = 5;

    public enum WorldState {
        JOIN("JOIN", 0),
        BROADCAST("BROADCAST", 1),
        NEGOTIATE("NEGOTIATE", 2),
        MOVE("MOVE", 3),
        NONE("NONE", -1);

        public final String value;
        public final int key;

        WorldState(String value, int key)
        {
            this.value = value.toUpperCase();
            this.key   = key;
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
