/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.ozu.drone.client.ui;

import edu.ozu.drone.client.AgentHandler;
import edu.ozu.drone.utils.JSONWorldWatch;

import java.awt.*;

/**
 *
 * @author freedrone
 */
public class WorldWatch extends javax.swing.JPanel {

    private String agent_name;
    private AgentHandler client;
    private String server;
    private AgentUI parent;
    private String world_id;

    /**
     * Creates new form WorldWatch
     */
    public WorldWatch() {
        initComponents();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        canvas = new java.awt.Canvas();

        setBackground(new java.awt.Color(254, 254, 254));
        setToolTipText("");
        setPreferredSize(new java.awt.Dimension(400, 300));

        canvas.setPreferredSize(new java.awt.Dimension(400, 300));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(canvas, javax.swing.GroupLayout.DEFAULT_SIZE, 380, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(canvas, javax.swing.GroupLayout.DEFAULT_SIZE, 280, Short.MAX_VALUE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    void setAgentName(String name) {
        this.agent_name = name;
    }

    void setClient(AgentHandler client) {
        this.client = client;
    }

    void setServer(String server) {
        this.server = server;
    }

    void setParent(AgentUI ui) {
        this.parent = ui;
    }
    
    void setWorldID(String world_id) {
        this.world_id = world_id;
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private java.awt.Canvas canvas;
    // End of variables declaration//GEN-END:variables

    public void mount()
    {
        System.out.println("> " + client + " WorldWatch mount");
        client.setWatchUIRef(this);
        client.join(world_id);
    }

    public void unmount()
    {
        System.out.println("> " + client + " WorldWatch unmount");
        if (client != null)
            client.leave();
    }

    public void draw(JSONWorldWatch watch)
    {
        System.out.println(">:"+watch);
        canvas.setBackground(new java.awt.Color(195, 224, 254));
    }
}
