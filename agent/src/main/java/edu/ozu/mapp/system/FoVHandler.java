package edu.ozu.mapp.system;

import edu.ozu.mapp.dataTypes.Constraint;
import edu.ozu.mapp.utils.Globals;
import edu.ozu.mapp.utils.Point;

public class FoVHandler {
    public enum FoVTYPE {
        SQUARE,
        CIRCULAR
    }

    private WorldOverseer overseer;
    private FoVTYPE foVBehaviour = Globals.FIELD_OF_VIEW_TYPE;

    public FoVHandler(WorldOverseer worldOverseer) {
        this.overseer = worldOverseer;
    }

    private FoV square(String agent_name)
    {
        FoV fov = new FoV();
        Point loc = new Point(this.overseer.agent_to_point.get(agent_name), "-");

        for (int i = 0; i < Globals.FIELD_OF_VIEW_SIZE; i++) {
            for (int j = 0; j < Globals.FIELD_OF_VIEW_SIZE; j++) {
                int x = loc.x + (j - Globals.FIELD_OF_VIEW_SIZE / 2);
                int y = loc.y + (i - Globals.FIELD_OF_VIEW_SIZE / 2);

                if (x == loc.x && y == loc.y) {
                    continue;
                }

                String agent_key = this.overseer.point_to_agent.getOrDefault(x + "-" + y, "");
                if (agent_key.isEmpty()) {
                    continue;
                }

                Broadcast broadcast = new Broadcast();
                broadcast.agent_name = agent_key;
                if (this.overseer.passive_agents.containsKey(agent_key)) {
                    if (this.overseer.passive_agents.get(agent_key)[1].equals("")) {
                        continue;
                    }

                    broadcast.add(new Constraint(agent_key, new Point(x, y), this.overseer.passive_agents.get(agent_key)[1]));
                    fov.broadcasts.add(broadcast);
                    continue;
                }

                String[] agent_broadcast = this.overseer.broadcasts.get(agent_key);
                for (int _t = 0; _t < agent_broadcast.length; _t++) {
                    broadcast.add(new Constraint(agent_key, new Point(agent_broadcast[_t], "-"), _t + this.overseer.TIME));
                }
                fov.broadcasts.add(broadcast);
            }
        }

        return fov;
    }

    public FoV handler(String agent_name)
    {
        switch (foVBehaviour)
        {
            case CIRCULAR:
                throw new UnsupportedOperationException();
            case SQUARE:
            default:
                return square(agent_name);
        }
    }
}
