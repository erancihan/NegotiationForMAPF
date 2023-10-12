package edu.ozu.mapp.utils;

import edu.ozu.mapp.system.fov.FoVHandler;
import edu.ozu.mapp.system.LeaveActionHandler;
import edu.ozu.mapp.utils.bid.BidSpace;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Globals
{
    public static       int     MAX_COMMITMENT_SIZE = Integer.MAX_VALUE;
    public static       int     BROADCAST_SIZE          = 9;
    public static final String  SERVER                  = "localhost:3001";
    public static final String  REDIS_HOST              = "localhost";
    public static final int     REDIS_PORT              = 6379;
    public static final int     INITIAL_TOKEN_BALANCE   = 5;
    public static       int     FIELD_OF_VIEW_SIZE      = 9;
    public static       long    NEGOTIATION_DEADLINE_MS = Long.MAX_VALUE;
    public static       int     NEGOTIATION_DEADLINE_ROUND = Integer.MAX_VALUE;
    public static       LeaveActionHandler.LeaveActionTYPE LEAVE_ACTION_BEHAVIOUR = LeaveActionHandler.LeaveActionTYPE.PASS_THROUGH;
    public static final FoVHandler.FoVTYPE FIELD_OF_VIEW_TYPE = FoVHandler.FoVTYPE.SQUARE;
    public static       int     MOVE_ACTION_SPACE_SIZE  = 5;
    public static       int     MAX_BID_SPACE_POOL_SIZE = 300;
    public static       int     MAX_BID_SPACE_POLL_COUNT = 300;
    public static       BidSpace.SearchStrategy BID_SEARCH_STRATEGY_OVERRIDE = null;
    public static       int     STALE_NEGOTIATE_STATE_WAIT_COUNTER_LIMIT     = 60;
    public static       int     STALE_SIMULATION_PROCESS_COUNTER_LIMIT = 300;
    public static       boolean LOG_BID_SPACE           = false;

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
