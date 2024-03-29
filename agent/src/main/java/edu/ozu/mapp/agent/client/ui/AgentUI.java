/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.ozu.mapp.agent.client.ui;

import edu.ozu.mapp.agent.client.AgentHandler;
import org.springframework.util.Assert;

import javax.swing.*;
import java.awt.*;

/**
 *
 * @author freedrone
 */
public class AgentUI extends javax.swing.JFrame {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(AgentUI.class);

    private AgentHandler client;

    /**
     * Creates new form AgentUI
     * @param client : reference to AgentClient class that invoked UI
     */
    public AgentUI(AgentHandler client) {
        Assert.notNull(client, "AgentHandler cannot be null");
        this.client = client;

        logger.info("init");

        initComponents();
        onComponentsDidMount();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        javax.swing.JPanel agent_info = new javax.swing.JPanel();
        agent_name = new javax.swing.JLabel();
        worlds_info_container = new javax.swing.JPanel();
        worlds_list = new edu.ozu.mapp.agent.client.ui.WorldsPanel();
        world_watch = new edu.ozu.mapp.agent.client.ui.WorldWatch();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                formComponentResized(evt);
            }
        });
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosed(java.awt.event.WindowEvent evt) {
                onWindowClosed(evt);
            }
        });

        agent_info.setMinimumSize(new java.awt.Dimension(100, 100));
        agent_info.setPreferredSize(new java.awt.Dimension(89, 30));
        agent_info.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        agent_name.setText("agent-name");
        agent_info.add(agent_name);

        getContentPane().add(agent_info, java.awt.BorderLayout.PAGE_START);

        worlds_info_container.setPreferredSize(new java.awt.Dimension(400, 300));
        worlds_info_container.setLayout(new java.awt.CardLayout());
        worlds_info_container.add(worlds_list, "worlds_list");

        world_watch.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentHidden(java.awt.event.ComponentEvent evt) {
                world_watchComponentHidden(evt);
            }
        });
        worlds_info_container.add(world_watch, "world_watch");

        getContentPane().add(worlds_info_container, java.awt.BorderLayout.CENTER);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void onWindowClosed(java.awt.event.WindowEvent event) {//GEN-FIRST:event_onWindowClosed
        logger.info("window closed");

        this.client.exit();
    }//GEN-LAST:event_onWindowClosed

    private void world_watchComponentHidden(java.awt.event.ComponentEvent event) {//GEN-FIRST:event_world_watchComponentHidden
//        System.out.println("> " + this.client.getClass().getName() + " world_watch hidden");
        world_watch.unmount();
    }//GEN-LAST:event_world_watchComponentHidden

    private void formComponentResized(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_formComponentResized
//        System.out.println(">::(w" + evt.getComponent().getWidth() + ",h" + evt.getComponent().getHeight() + ")");
    }//GEN-LAST:event_formComponentResized

//<editor-fold defaultstate="collapsed" desc="ignore main">
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(AgentUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> new AgentUI(null).setVisible(true));
    }
//</editor-fold>

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel agent_name;
    private edu.ozu.mapp.agent.client.ui.WorldWatch world_watch;
    private javax.swing.JPanel worlds_info_container;
    private edu.ozu.mapp.agent.client.ui.WorldsPanel worlds_list;
    // End of variables declaration//GEN-END:variables

    private void onComponentsDidMount()
    {
        agent_name.setText(client.getAgentName());

        loadWorldsList();
    }

    public void join(String world_id)
    {
        // switch to watch ui
        CardLayout cl = (CardLayout) worlds_info_container.getLayout();
        cl.show(worlds_info_container, "world_watch");
        world_watch.setClient(client);
        world_watch.setWorldID(world_id);
        world_watch.mount();

        this.setSize(600, 367);
    }

    private void loadWorldsList()
    {
        CardLayout cl = (CardLayout) worlds_info_container.getLayout();
        cl.show(worlds_info_container, "worlds_list");
        worlds_list.setClientRef(client);
        worlds_list.setParent(this);
        worlds_list.loadList();

        this.setSize(400, 367);
    }
}
