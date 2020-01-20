package edu.ozu.drone.utils;

import com.sun.corba.se.impl.interceptors.PICurrent;

public class Point {
    public int x, y;
    public String key;

    public Point(int x, int y) {
        this.x = x;
        this.y = y;

        this.key = x + "-" + y;
    }

    public Point(String agent_position_datum, String delim)
    {
        String[] pos = agent_position_datum.split(delim);

        x = Integer.parseInt(pos[0]);
        y = Integer.parseInt(pos[1]);

        key = x + "-" + y;
    }

    public double ManhattanDistTo(Point to) {
        return Math.abs(x - to.x) + Math.abs(y - to.y);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Point)
            return (x == ((Point) o).x) && (y == ((Point) o).y);
        else
            return false;
    }
}
