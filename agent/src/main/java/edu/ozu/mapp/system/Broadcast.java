package edu.ozu.mapp.system;

import edu.ozu.mapp.dataTypes.Constraint;

import java.util.ArrayList;

public class Broadcast {
    public ArrayList<Constraint> locations;
    public String agent_name;

    public void add(Constraint constraint) {
        locations.add(constraint);
    }

    public String[] getPathStringArray() {
        String[] path = new String[locations.size()];

        for (int i = 0; i < locations.size(); i++) {
            path[i] = locations.get(i).location.key;
        }

        return path;
    }

    @Override
    public String toString() {
        return "[" + agent_name + ' ' + locations + ']';
    }
}
