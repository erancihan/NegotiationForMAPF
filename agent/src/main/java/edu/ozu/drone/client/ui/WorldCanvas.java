/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.ozu.drone.client.ui;

import edu.ozu.drone.utils.JSONWorldWatch;
import edu.ozu.drone.utils.Point;

import java.awt.*;
import java.util.Arrays;

/**
 *
 * @author freedrone
 */
public class WorldCanvas extends Canvas {
    private JSONWorldWatch data;
    private Point xy_own;

    @Override
    public void paint(Graphics graphics)
    {
        super.paint(graphics);
        if (data == null || data.fov == null) { return; } // data can be null, dont proceed

        System.out.println(">:" + Arrays.deepToString(data.fov) + " " + xy_own);
    }

    void setData(JSONWorldWatch data, Point agent_position)
    {
        this.data = data;
        this.xy_own = agent_position;
        this.repaint();
    }
}
