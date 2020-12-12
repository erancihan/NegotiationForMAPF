package edu.ozu.mapp.agent.client.helpers;

import java.util.HashMap;

public class ConflictCheck {

    public enum ConflictType
    {
        VertexConflict, SwapConflict, NONE;
    }

    /**
     * Check for conflicts in given two paths.
     * Returns index of conflict, -1 otherwise
     *
     * @return ConflictInfo
     * */
    public ConflictInfo[] GetAllConflicts(String[] own, String[] other)
    {
        HashMap<String, ConflictInfo> conflicts = new HashMap<>();

        // check Vertex Conflict
        for (int i = 0; i < own.length && i < other.length; i++)
        {
            if (own[i].trim().equals(other[i].trim()))
            {
                conflicts.put(own[i], new ConflictInfo(i, ConflictType.VertexConflict));
                break;  // finding 1 per path is enough
            }
        }

        // check Swap Conflict
        for (int t = 0; t + 1 < own.length && t + 1 < other.length; t++)
        {
            if (own[t].trim().equals(other[t + 1].trim()) && own[t+1].trim().equals(other[t].trim()))
            {
                conflicts.put(own[t]+own[t+1], new ConflictInfo(t, ConflictType.SwapConflict));
                break;  // finding 1 per path is enough
            }
        }

        if (conflicts.size() > 0) {
            System.out.println(">>" + conflicts);
        }

        return conflicts.values().toArray(new ConflictInfo[0]);
    }

    public ConflictInfo check(String[] own, String[] other)
    {
        ConflictInfo[] conflicts = GetAllConflicts(own, other);
        if (conflicts.length == 0) {
            return new ConflictInfo(ConflictType.NONE);
        }

        return GetAllConflicts(own, other)[0];
    }
}

