package edu.ozu.mapp.dataTypes;

public class CollisionCheckResult
{
    public enum Type {
        OBSTACLE, COLLISION, NONE;
    }

    public final Type type;
    public int index;
    public String[] agent_ids;
    public String conflict_location;

    public CollisionCheckResult() {
        this.type = Type.NONE;
    }

    public CollisionCheckResult(int index, Type type)
    {
        this.index = index;
        this.type = type;
    }

    public CollisionCheckResult(Type type) {
        this.type = type;
    }
}
