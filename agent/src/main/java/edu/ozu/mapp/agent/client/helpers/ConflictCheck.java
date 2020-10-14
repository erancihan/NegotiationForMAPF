package edu.ozu.mapp.agent.client.helpers;

public class ConflictCheck {

    public enum ConflictType
    {
        VertexConflict, SwapConflict, NONE;
    }

    /**
     * Check for conflicts in given two paths.
     * Returns index of conflict, -1 otherwise
     *
     * @return int
     * */
    public ConflictInfo check(String[] own, String[] other)
    {
        // check Vertex Conflict
        for (int i = 0; i < own.length && i < other.length; i++)
        {
            if (own[i].equals(other[i]))
            {
                return new ConflictInfo(i, ConflictType.VertexConflict);
            }
        }

        // check Swap Conflict
        for (int t = 0; t + 1 < own.length && t < other.length; t++)
        {
            if (own[t].equals(other[t]))
            {
                return new ConflictInfo(t, ConflictType.SwapConflict);
            }
        }

        return new ConflictInfo(-1, ConflictType.NONE);
    }
}

