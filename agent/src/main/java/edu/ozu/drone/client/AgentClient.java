package edu.ozu.drone.client;

import edu.ozu.drone.agent.Agent;
import edu.ozu.drone.client.ui.AgentUI;
import org.springframework.util.Assert;

import javax.swing.*;

public class AgentClient {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(AgentClient.class);
    private AgentHandler handler;

    public AgentClient(String[] args, Agent agent)
    {
        Assert.notNull(agent, "agent cannot be null");

        agent.init();
        agent.run();

        handler = new AgentHandler(agent);

        if (!agent.isHeadless)
        {
            logger.info("Display UI");
            __launchUI();
        }
    }

    @SuppressWarnings("Duplicates")
    private void __launchUI()
    {
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html
         */
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(AgentUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> new AgentUI(this.handler).setVisible(true));
    }

    // handler hooks

    public void join(String worldID)
    {
        handler.join(worldID);
    }
}
