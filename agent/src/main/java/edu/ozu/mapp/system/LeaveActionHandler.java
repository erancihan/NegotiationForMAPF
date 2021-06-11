package edu.ozu.mapp.system;

import edu.ozu.mapp.agent.client.AgentHandler;
import edu.ozu.mapp.utils.Globals;

public class LeaveActionHandler {
    public enum LeaveActionTYPE {
        OBSTACLE,
        PASS_THROUGH
    }

    WorldOverseer overseer;
    LeaveActionTYPE leaveBehaviour = Globals.LEAVE_ACTION_BEHAVIOUR;

    public LeaveActionHandler(WorldOverseer worldOverseer) {
        overseer = worldOverseer;
    }

    public void setLeaveBehaviour(LeaveActionTYPE type) {
        this.leaveBehaviour = type;
    }

    public void handle(AgentHandler agent) {
        this.overseer.FLAG_COLLISION_CHECKS.remove(agent.GetAgentID());
        this.overseer.FLAG_INACTIVE.put(agent.GetAgentID(), "");

        switch (leaveBehaviour)
        {
            case PASS_THROUGH:
                // mark agent passive w/ empty conflict time
                this.overseer.passive_agents.put(agent.GetAgentID(), new String[]{ agent.GetCurrentLocation(), "" });
                // detach agent from point to agent & agent to point
                this.overseer.point_to_agent.put(agent.GetCurrentLocation(), "");
                break;
            case OBSTACLE:
            default:
                this.overseer.passive_agents.put(agent.GetAgentID(), new String[]{ agent.GetCurrentLocation(), "inf" });
                break;
        }

        this.overseer.active_agent_c--;
    }
}
