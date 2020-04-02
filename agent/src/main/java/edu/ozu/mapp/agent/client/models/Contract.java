package edu.ozu.mapp.agent.client.models;

import edu.ozu.mapp.keys.KeyHandler;

import java.util.Map;

public class Contract {
    public String Ox;
    public String x;
    public String ETa;
    public String a; // id of agent A
    public String ETb;
    public String b; // id of agent B

    public Contract(Map<String, String> sess) {
        Ox = sess.getOrDefault("Ox", "");
        x = sess.getOrDefault("x", "");
        ETa = sess.getOrDefault("ETa", "");
        ETb = sess.getOrDefault("ETb", "");
    }

    public String getToken(String AgentID) {
        if (a.equals(AgentID)) {
            return KeyHandler.decrypt(ETa, KeyHandler.getPubKey(AgentID));
        }
        if (b.equals(AgentID)) {
            return KeyHandler.decrypt(ETb, KeyHandler.getPubKey(AgentID));
        }

        return "";
    }

    public void apply() {

    }
}
