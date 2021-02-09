package mapp.agent;

import edu.ozu.mapp.agent.client.models.Contract;
import edu.ozu.mapp.utils.Action;
import edu.ozu.mapp.utils.Point;

@edu.ozu.mapp.agent.MAPPAgent
public class AgentTest extends edu.ozu.mapp.agent.Agent {
    public AgentTest(String agentName, String agentID, Point start, Point dest) {
        super(agentName, agentID, start, dest);
    }

    @Override
    public Action onMakeAction(Contract contract) {
        return null;
    }
}
