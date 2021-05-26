package edu.ozu.mapp.system;

import edu.ozu.mapp.agent.client.AgentHandler;
import edu.ozu.mapp.utils.Globals;

public class MoveActionHandler {
    public enum MoveActionSpace {
        UP(0, -1), DOWN(0, 1), LEFT(-1, 0), RIGHT(1, 0),
        NORTH(0, -1), SOUTH(0, 1), WEST(-1, 0), EAST(1, 0),
        WAIT, NONE
        ;

        public int dx = 0;
        public int dy = 0;
        MoveActionSpace(int dx, int dy) {
            this.dx = dx; this.dy = dy;
        }

        MoveActionSpace() {dx = 0; dy = 0;}
    }

    WorldOverseer overseer;

    public static MoveActionSpace[] getActionSpace() {
        switch (Globals.MOVE_ACTION_SPACE_SIZE)
        {
            case 5:
                return new MoveActionSpace[] {
                        MoveActionSpace.UP,
                        MoveActionSpace.DOWN,
                        MoveActionSpace.LEFT,
                        MoveActionSpace.RIGHT,
                        MoveActionSpace.WAIT
                };
            case 4:
            default:
                return new MoveActionSpace[] {
                        MoveActionSpace.UP,
                        MoveActionSpace.DOWN,
                        MoveActionSpace.LEFT,
                        MoveActionSpace.RIGHT
                };
        }
    }


    public MoveActionHandler(WorldOverseer worldOverseer)
    {
        this.overseer = worldOverseer;
    }

    public void handle(AgentHandler agent, DATA_REQUEST_PAYLOAD_WORLD_MOVE payload)
    {
        // queue agent for movement
        this.overseer.movement_handler.put(agent.GetAgentID(), agent, payload);
    }
}
