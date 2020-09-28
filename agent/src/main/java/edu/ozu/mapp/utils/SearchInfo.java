package edu.ozu.mapp.utils;

public class SearchInfo {
    public Path Path;
    public Double MinPathSize;
    public Double MaxPathSize;
    public Double PathSize;

    public SearchInfo(double max, double min, Path path) {
        MaxPathSize = max;
        MinPathSize = min;
        PathSize = (double) path.size();
        Path = path;
    }

    @Override
    public String toString() {
        return "SearchInfo{" +
                "Path=" + Path +
                ", MinPathSize=" + MinPathSize +
                ", MaxPathSize=" + MaxPathSize +
                ", PathSize=" + PathSize +
                '}';
    }
}
