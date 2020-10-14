package edu.ozu.mapp.agent.client.helpers;

public class ConflictInfo {
    public boolean hasConflict;

    public ConflictCheck.ConflictType type;
    public int index;

    public ConflictInfo(int index, ConflictCheck.ConflictType type)
    {
        this.index = index;
        this.type = type;

        hasConflict = index >= 0;
    }
}
