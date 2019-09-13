/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.ozu.drone.client.ui;

import edu.ozu.drone.client.AgentClient;

/**
 *
 * @author freedrone
 */
public class WorldWatch extends javax.swing.JPanel {

    private String agent_name;
    private AgentClient client;
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

        jButton1 = new javax.swing.JButton();

        setBackground(new java.awt.Color(254, 254, 254));
        setToolTipText("");
        setPreferredSize(new java.awt.Dimension(400, 300));
        setLayout(new java.awt.GridBagLayout());

        jButton1.setForeground(new java.awt.Color(255, 98, 0));
        jButton1.setText("asdfasdf");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });
        add(jButton1, new java.awt.GridBagConstraints());
    }// </editor-fold>//GEN-END:initComponents

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        parent.loadWorldsList();
    }//GEN-LAST:event_jButton1ActionPerformed

    void setAgentName(String name) {
        this.agent_name = name;
    }

    void setClient(AgentClient client) {
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
    private javax.swing.JButton jButton1;
    // End of variables declaration//GEN-END:variables

    public void mount() {
        System.out.println("> " + client + " WorldWatch mount");
        client.setWatchRef(this);
        client.join(server);
    }

    public void unmount() {
        System.out.println("> " + client + " WorldWatch unmount");
    }
}
