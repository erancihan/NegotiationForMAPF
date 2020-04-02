package edu.ozu.mapp.keys;

import edu.ozu.mapp.agent.Agent;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

public class AgentKeys {
    private String _agentID;

    private PrivateKey _private;
    private PublicKey _public;

    public AgentKeys(PrivateKey aPrivate, PublicKey aPublic, Agent agent) {
        _agentID = agent.AGENT_ID;

        _private = aPrivate;
        _public = aPublic;
    }

    public AgentKeys(KeyPair pair, Agent agent) {
        _agentID = agent.AGENT_ID;

        _private = pair.getPrivate();
        _public = pair.getPublic();
    }

    public PrivateKey getPrivateKey(Agent agent) {
        if (_agentID.equals(agent.AGENT_ID))
        {   // TODO this should be instance check, not ID check!!!
            return _private;
        }
        return null;
    }

    public PublicKey get_public() {
        return _public;
    }
}
