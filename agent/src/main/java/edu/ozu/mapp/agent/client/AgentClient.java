package edu.ozu.mapp.agent.client;

import edu.ozu.mapp.agent.Agent;
import edu.ozu.mapp.agent.client.ui.AgentUI;
import org.springframework.util.Assert;

import javax.swing.*;

public class AgentClient {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(AgentClient.class);
    private AgentHandler handler;

    public AgentClient(Agent agent)
    {
        this(new String[0], agent);
        logger.debug("agent hash:" + agent.hashCode() + " client hash:" + this.hashCode());
    }

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
        } else {
            logger.info(agent.AGENT_ID + " is headless! waiting for join hook...");
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

    // Handler Hooks

    public void join(String worldID)
    {
        handler.join(worldID);
    }

    public void leave()
    {
        handler.leave();
    }
}
