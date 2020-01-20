/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.ozu.drone.client.ui;

import edu.ozu.drone.utils.JSONWorldWatch;

import java.awt.*;

/**
 *
 * @author freedrone
 */
public class WorldCanvas extends Canvas {
    private JSONWorldWatch data;
    private String[] own_path;

    private int r = 0;
    private int fov_center;
    private int offset = 5; // set offset

    @Override
    public void paint(Graphics graphics)
    {
        super.paint(graphics);
        if (data == null || data.fov == null) { return; } // data can be null, dont proceed

        graphics.setColor(Color.WHITE);
        graphics.fillRect(offset, offset, (r * data.fov_size), (r * data.fov_size));

        edu.ozu.drone.utils.Point xy_own = new edu.ozu.drone.utils.Point(own_path[0], "-");
        for (String[] agent_data : data.fov)
        { // draw agents
            String name = agent_data[0];
            String[] xy = agent_data[1].split(":");
            String[] path = agent_data[2].replaceAll("([\\[\\]]*)", "").split(",");

            int ox = (Integer.parseInt(xy[0]) - xy_own.x) + fov_center;
            int oy = (Integer.parseInt(xy[1]) - xy_own.y) + fov_center;

            if (ox == fov_center && oy == fov_center)
                graphics.setColor(new java.awt.Color(39, 145, 60));
            else
                graphics.setColor(Color.BLUE);
            graphics.fillOval((offset + (ox*r) + 5), (offset + (oy*r) + 5), (r - 10), (r - 10));
        }

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

        if ((xy_own.x - fov_center) < 0 || (xy_own.y - fov_center) < 0)
        { // draw walls
            for (int y = 0; y < data.fov_size; y++)
                for (int x = 0; x < data.fov_size; x++)
                {
                    int cx = xy_own.x - fov_center + x;
                    int cy = xy_own.y - fov_center + y;

                    if (cx >= 0 && cy >= 0) { continue; }

                    graphics.setColor(new java.awt.Color(56, 56, 56));
                    graphics.fillRect((offset + (x * r) + 1), (offset + (y * r) + 1), (r - 1), (r - 1));
                }
        }
    }

    void setData(JSONWorldWatch data, String[] agent_position_data)
    {
        this.data = data;
        this.own_path = agent_position_data;

        this.r = this.getWidth() > this.getHeight()
                ? (this.getHeight() - 2 * offset) / (data.fov_size)
                : (this.getWidth() - 2 * offset) / (data.fov_size);

        this.fov_center = data.fov_size / 2;

        this.repaint();
    }
}
