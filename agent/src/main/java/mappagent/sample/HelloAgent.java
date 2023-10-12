package mappagent.sample;

import edu.ozu.mapp.agent.Agent;
import edu.ozu.mapp.agent.MAPPAgent;
import edu.ozu.mapp.agent.client.AgentClient;
import edu.ozu.mapp.agent.client.models.Contract;
import edu.ozu.mapp.utils.*;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("Duplicates")
@MAPPAgent
public class HelloAgent extends Agent
{
    public HelloAgent()
    {
        this("Hello Agent", "S006486", new Point(2, 0), new Point(2, 10));

        logger.info("loading hello agent");
        isHeadless = false;
    }

    public HelloAgent(String agentName, String agentID, Point start, Point dest)
    {
        super(agentName, agentID, start, dest);
    }

    @Override
    public void PreNegotiation(State state) {

    }

    @Override
    public Action onMakeAction(Contract contract)
    {

        return new Action(this, ActionType.ACCEPT);
    }

    @Override
    public void PostNegotiation(Contract contract) {
        logger.debug("history:" + history);
    }

    public static void main(String[] args)
    {
//        new AgentClient(args, new HelloAgent());
    }
}
