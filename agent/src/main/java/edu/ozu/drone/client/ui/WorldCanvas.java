/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.ozu.drone.client.ui;

import edu.ozu.drone.utils.JSONWorldWatch;
import edu.ozu.drone.utils.Point;

import java.awt.*;

/**
 *
 * @author freedrone
 */
public class WorldCanvas extends Canvas {
    private JSONWorldWatch data;
    private Point xy_own;

    @Override
    public void paint(Graphics graphics) {
        super.paint(graphics);
        System.out.println(">:" + data);
    }

    void setData(JSONWorldWatch data, Point agent_position)
    {
        this.data = data;
        this.xy_own = agent_position;
        this.repaint();
    }
}
