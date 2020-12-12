package edu.ozu.mapp.agent.client.helpers;

import edu.ozu.mapp.agent.Agent;
import edu.ozu.mapp.agent.client.models.Contract;
import edu.ozu.mapp.utils.Globals;
import redis.clients.jedis.Jedis;

import java.util.HashMap;
import java.util.Map;

public class Negotiation {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Negotiation.class);

    public Contract getContract(String WORLD_ID, String AGENT_ID)
    {
        Map<String, String> sess = new HashMap<>();
        try
        {
            Jedis jedis = new Jedis(Globals.REDIS_HOST, Globals.REDIS_PORT);

            String session_id = jedis.hget("world:"+WORLD_ID+":notify", "agent:"+AGENT_ID);
            if (session_id.split(",").length > 1)
            {
                logger.error("something went wrong!");
                System.exit(1);
            }
            sess = jedis.hgetAll("negotiation:"+session_id);
            sess.put("_session_id", session_id);

            logger.debug("{session_id:"+session_id+"}");
            logger.debug(sess.toString());

            jedis.close();
        } catch (Exception e) {
            logger.error("Could not get contract " + WORLD_ID + " | " + AGENT_ID);
            e.printStackTrace();
            System.exit(1);
        }

        return Contract.Create(sess);
    }

    public Contract getContract(Agent agent)
    {
        return getContract(agent.WORLD_ID, agent.AGENT_ID);
    }
}
