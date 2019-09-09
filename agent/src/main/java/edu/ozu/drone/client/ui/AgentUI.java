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
public class AgentUI extends javax.swing.JFrame {

    AgentClient client;

    /**
     * Creates new form AgentUI
     * @param client
     */
    public AgentUI(AgentClient client) {
        this.client = client;

        System.out.println(this.client.getClass().getName());

        initComponents();
        
        onComponentDidMount();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        agent_info = new javax.swing.JPanel();
        agent_name = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        worldsPanel1 = new edu.ozu.drone.client.ui.WorldsPanel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosed(java.awt.event.WindowEvent evt) {
                onWindowClosed(evt);
            }
        });

        agent_info.setMinimumSize(new java.awt.Dimension(100, 100));

        agent_name.setText("agent-name");
        agent_info.add(agent_name);

        getContentPane().add(agent_info, java.awt.BorderLayout.PAGE_START);

        jPanel1.setPreferredSize(new java.awt.Dimension(400, 300));
        jPanel1.setLayout(new java.awt.CardLayout());
        jPanel1.add(worldsPanel1, "card2");

        getContentPane().add(jPanel1, java.awt.BorderLayout.CENTER);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void onWindowClosed(java.awt.event.WindowEvent event) {//GEN-FIRST:event_onWindowClosed
        // TODO add your handling code here:
        System.out.println("exiting " + client.getClass().getName());

        client.exit();
    }//GEN-LAST:event_onWindowClosed

//<editor-fold defaultstate="collapsed" desc="ignore main">
//    /**
//     * @param args the command line arguments
//     */
//    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
/*
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(AgentUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(AgentUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(AgentUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(AgentUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
 */
        //</editor-fold>

        /* Create and display the form */
//        java.awt.EventQueue.invokeLater(new Runnable() {
//            public void run() {
//                new AgentUI().setVisible(true);
//            }
//        });
//    }
//</editor-fold>
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel agent_info;
    private javax.swing.JLabel agent_name;
    private javax.swing.JPanel jPanel1;
    private edu.ozu.drone.client.ui.WorldsPanel worldsPanel1;
    // End of variables declaration//GEN-END:variables

    private void onComponentDidMount() {
        agent_name.setText(client.AGENT_NAME);
    }
}