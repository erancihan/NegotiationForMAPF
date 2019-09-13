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

    @Override
    public void paint(Graphics graphics) {
        super.paint(graphics);
        System.out.println(">:" + data);
    }

    void setData(JSONWorldWatch data)
    {
        this.data = data;
        this.repaint();
    }
}
