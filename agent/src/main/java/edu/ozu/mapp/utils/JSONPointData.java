package edu.ozu.mapp.utils;

import org.springframework.util.Assert;

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

    public Point toPoint() {
        return new Point(x, y);
    }

    public void update(String val)
    {
        String[] xy = val.split(",");
        Assert.isTrue(xy.length == 2, "wrong number of values");

        this.x = Integer.parseInt(xy[0]);
        this.y = Integer.parseInt(xy[1]);
    }

    @Override
    public String toString()
    {
        return "(" + x + ", " + y + ")";
    }
}
