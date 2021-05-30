/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.ozu.mapp.agent.client.ui;

import edu.ozu.mapp.system.Broadcast;
import edu.ozu.mapp.utils.JSONWorldWatch;
import edu.ozu.mapp.utils.Point;

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

        edu.ozu.mapp.utils.Point xy_own = new edu.ozu.mapp.utils.Point(own_path[0], "-");
        for (Broadcast broadcast : data.fov.broadcasts)
        {   // draw agents
            Point xy = broadcast.locations.get(0).location;

            int ox = (xy.x - xy_own.x) + fov_center;
            int oy = (xy.y - xy_own.y) + fov_center;

            if (ox == fov_center && oy == fov_center)
                graphics.setColor(new java.awt.Color(39, 145, 60));
            else
                graphics.setColor(Color.BLUE);
            graphics.fillOval((offset + (ox*r) + 5), (offset + (oy*r) + 5), (r - 10), (r - 10));

            // draw path
            if (broadcast.locations.size() == 1)
            {   // own path, replace path data
                // TODO: ... WHAT?!?!
//                path = own_path;
            }

            for (int i = 0; i + 1 < broadcast.locations.size(); i++)
            {// draw path
                Point from = broadcast.locations.get(i).location;
                Point dest = broadcast.locations.get(i+1).location;

                int f_x = (from.x - xy_own.x) + fov_center;
                int f_y = (from.y - xy_own.y) + fov_center;
                int d_x = (dest.x - xy_own.x) + fov_center;
                int d_y = (dest.y - xy_own.y) + fov_center;

                graphics.drawLine(
                        offset + (f_x * r) + r/2,
                        offset + (f_y * r) + r/2,
                        offset + (d_x * r) + r/2,
                        offset + (d_y * r) + r/2
                );
            }
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

        for (int y = 0; y < data.fov_size; y++)
        {
            for (int x = 0; x < data.fov_size; x++)
            {
                int cx = xy_own.x - fov_center + x;
                int cy = xy_own.y - fov_center + y;

                // draw coordinates
                graphics.setColor(Color.RED);
                graphics.drawString(cx + "," + cy, (offset + (x * r) + 2), (offset + (y * r) + 13));

                if (cx < 0 || cy < 0)
                {   // draw walls
                    graphics.setColor(new java.awt.Color(56, 56, 56));
                    graphics.fillRect((offset + (x * r) + 1), (offset + (y * r) + 1), (r - 1), (r - 1));
                }
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
