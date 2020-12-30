package edu.ozu.mapp.system;

import java.util.*;

public class Colors
{
    private static final HashSet<String> hexes = new HashSet<String>(
            Arrays.asList(
                    "#3366CC",  // 0
                    "#DC3912",
                    "#FF9900",
                    "#109618",
                    "#990099",
                    "#3B3EAC",
                    "#0099C6",
                    "#DD4477",
                    "#66AA00",
                    "#B82E2E",
                    "#316395",
                    "#994499",
                    "#22AA99",
                    "#AAAA11",
                    "#6633CC",
                    "#E67300",
                    "#8B0707",
                    "#329262",
                    "#5574A6",
                    "#4A9CD0",
                    "#F97842",
                    "#A4A4A4",
                    "#FFBC40",
                    "#3774BD",
                    "#63A854",
                    "#0C608B",
                    "#A14421",
                    "#626262",
                    "#9D7026",
                    "#224774",
                    "#3F6735",
                    "#70AFD8",
                    "#FB9363",
                    "#B6B6B6",
                    "#FFC952",
                    "#608ECB",
                    "#81C071",
                    "#007EBC",
                    "#DD5C2B",
                    "#838383",
                    "#D29633",
                    "#295D9B",
                    "#528945"  // 42
            ));

    public static String get(int i)
    {
        int idx = 0;
        for (String val : hexes)
        {
            if (i == idx) return val;
            idx++;
        }

        Random rand = new Random();
        String hex_color;
        do {
            // get an unused random hex color
            hex_color = rgb2hex(rand.nextInt(256), rand.nextInt(256), rand.nextInt(256));
        } while (hex_color.isEmpty() && hexes.contains(hex_color));

        hexes.add(hex_color);

        return hex_color;
    }

    public static java.awt.Color hex2rgb(String hex)
    {
        if (hex == null) return null;

        return new java.awt.Color(
                Integer.valueOf(hex.substring(1, 3), 16),
                Integer.valueOf(hex.substring(3, 5), 16),
                Integer.valueOf(hex.substring(5, 7), 16)
        );
    }

    public static String rgb2hex(int r, int g, int b)
    {
        return String.format("#%02x%02x%02x", r, g, b);
    }
}
