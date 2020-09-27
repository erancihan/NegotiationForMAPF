package edu.ozu.mapp.utils;

public class JSONWorldData {
    public String wid;
    public int width;
    public int height;
    public int min_path_len;
    public int min_d;

    public JSONWorldData() {
    }

    public JSONWorldData(String wid, int width, int height, int min_path_len, int min_d) {
        this.wid = wid;
        this.width = width;
        this.height = height;
        this.min_path_len = min_path_len;
        this.min_d = min_d;
    }

    @Override
    public String toString() {
        return "JSONWorldData{" +
                "wid='" + wid + '\'' +
                ", width=" + width +
                ", height=" + height +
                ", min_path_len=" + min_path_len +
                ", min_d=" + min_d +
                '}';
    }
}
