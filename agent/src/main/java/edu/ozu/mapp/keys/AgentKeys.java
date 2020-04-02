package edu.ozu.mapp.keys;

import java.security.PrivateKey;
import java.security.PublicKey;

public class AgentKeys {
    public PrivateKey privateKey;
    public PublicKey publicKey;

    public AgentKeys(PrivateKey aPrivate, PublicKey aPublic) {
        privateKey = aPrivate;
        publicKey = aPublic;
    }
}
