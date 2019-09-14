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

    private int r = 0;
    private int offset = 5;

    @Override
    public void paint(Graphics graphics)
    {
        super.paint(graphics);
        if (data == null || data.fov == null) { return; } // data can be null, dont proceed

        graphics.setColor(Color.WHITE);
        graphics.fillRect(offset, offset, (r * data.fov_size), (r * data.fov_size));

        for (int i = 0; i <= data.fov_size; i++)
        { // draw vertical lines
            graphics.setColor(Color.BLACK);
            graphics.drawLine((offset + i*r), offset, (offset + i*r), (offset + r*data.fov_size));
        }
        for (int j = 0; j <= data.fov_size; j++)
        { // draw horizontal lines
            graphics.setColor(Color.BLACK);
            graphics.drawLine(offset, (offset + j*r), (offset + r*data.fov_size), (offset + j*r));
        }

        int dx = (int) (Math.floor(data.fov_size / 2.0) - xy_own.x);
        int dy = (int) (Math.floor(data.fov_size / 2.0) - xy_own.y);

        for (String[] agent_data : data.fov)
        {
            String name = agent_data[0];
            String[] xy = agent_data[1].split(":");
            String path = agent_data[2];
        }
    }

    void setData(JSONWorldWatch data, Point agent_position)
    {
        this.data = data;
        this.xy_own = agent_position;

        this.r = this.getWidth() > this.getHeight()
                ? this.getHeight() / (data.fov_size + 1)
                : this.getWidth() / (data.fov_size + 1);

        this.repaint();
    }
}
