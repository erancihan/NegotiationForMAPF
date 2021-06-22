package mappagent.sample;

import edu.ozu.mapp.agent.Agent;
import edu.ozu.mapp.agent.MAPPAgent;
import edu.ozu.mapp.agent.client.AgentClient;
import edu.ozu.mapp.agent.client.models.Contract;
import edu.ozu.mapp.system.FoV;
import edu.ozu.mapp.system.WorldOverseer;
import edu.ozu.mapp.utils.*;

import java.util.ArrayList;
import java.util.List;

@MAPPAgent
public class HelloAgent2 extends Agent
{
    public HelloAgent2()
    {
        this("Hello Agent2", "S006487", new Point(0, 2), new Point(10, 2));
        isHeadless = false;
    }

    public HelloAgent2(String agentName, String agentID, Point start, Point dest)
    {
        super(agentName, agentID, start, dest);
    }

    @Override
    public void PreNegotiation(State state) {

    }

    private int count = 0;

    @Override
    public Action onMakeAction(Contract contract)
    {
        return new Action(this, ActionType.ACCEPT);
    }

    public static void main(String[] args)
    {
//        new AgentClient(args, new HelloAgent2());
    }
}
