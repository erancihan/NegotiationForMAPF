package edu.ozu.drone;

public class Point {
    public int x, y;
    public String key;

    public Point(int x, int y) {
        this.x = x;
        this.y = y;

        this.key = x + "-" + y;
    }

    public double ManhattanDistTo(Point to) {
        return Math.abs(x - to.x) + Math.abs(y - to.y);
    }
}
