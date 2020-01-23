package edu.ozu.drone.utils;

public class Action
{
    public ActionType type;
    public String bid;

    public Action(ActionType type)
    {
        this(type, "");
    }

    public Action(ActionType type, String bid)
    {
        this.type = type;
        this.bid  = bid;
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
