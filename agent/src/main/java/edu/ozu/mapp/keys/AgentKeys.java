package edu.ozu.mapp.keys;

import java.security.PrivateKey;
import java.security.PublicKey;

public class AgentKeys {
    private String _agentID;

    private PrivateKey _private;
    private PublicKey _public;

    public AgentKeys(PrivateKey aPrivate, PublicKey aPublic, String agentID) {
        _agentID = agentID;
        _private = aPrivate;
        _public = aPublic;
    }

    public PrivateKey getPrivateKey(String AgentID) {
        if (_agentID.equals(AgentID))
        {   // TODO this should be instance check, not ID check!!!
            return _private;
        }
        return null;
    }

    public PublicKey get_public() {
        return _public;
    }
}
