package edu.ozu.mapp.utils;

public class JSONPointData {
    public int x;
    public int y;

    public JSONPointData(Point point) {
        this.x = point.x;
        this.y = point.y;
    }

    public String get() {
        return x+"-"+y;
    }
}
