package edu.ozu.drone.agent.sample;

import edu.ozu.drone.agent.Agent;
import edu.ozu.drone.utils.Action;
import edu.ozu.drone.utils.Point;

public class HelloAgent2 extends Agent {

    @Override
    public void init() {
        AGENT_NAME = "Hello Agent2";
        AGENT_ID   = "S006487";

        START = new Point(1, 0);
        DEST = new Point(1, 3);
    }

    @Override
    public Action onMakeAction() {
        return null;
    }

    @Override
    public void onReceiveState(edu.ozu.drone.utils.State state) {

    }
}
