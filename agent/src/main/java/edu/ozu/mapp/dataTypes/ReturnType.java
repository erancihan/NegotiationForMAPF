package edu.ozu.mapp.dataTypes;

public class ReturnType
{
    public enum Type {
        OBSTACLE, COLLISION, NONE;
    }

    public final Type type;
    public String[] agent_ids;

    public ReturnType() {
        this.type = Type.NONE;
    }

    public ReturnType(Type type) {
        this.type = type;
    }
}
