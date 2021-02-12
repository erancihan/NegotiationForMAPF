package edu.ozu.mapp.dataTypes;

public class ReturnType
{
    public enum Type {
        OBSTACLE, COLLISION, NONE;
    }

    public final Type type;
    public int index;
    public String[] agent_ids;
    public String conflict_location;

    public ReturnType() {
        this.type = Type.NONE;
    }

    public ReturnType(int index, Type type)
    {
        this.index = index;
        this.type = type;
    }

    public ReturnType(Type type) {
        this.type = type;
    }
}
