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

    public ConflictInfo(ConflictCheck.ConflictType type)
    {
        if (type == ConflictCheck.ConflictType.NONE)
        {
            this.index = -1;
            this.type = type;
            this.hasConflict = false;
        }
    }

    @Override
    public String toString() {
        return "ConflictInfo{type=" + type + ", index=" + index + '}';
    }
}
