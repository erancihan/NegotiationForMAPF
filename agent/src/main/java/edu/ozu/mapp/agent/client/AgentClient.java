package edu.ozu.mapp.agent.client;

import edu.ozu.mapp.agent.Agent;
import edu.ozu.mapp.agent.client.models.Contract;
import edu.ozu.mapp.agent.client.ui.AgentUI;
import edu.ozu.mapp.system.DATA_REQUEST_PAYLOAD_WORLD_JOIN;
import edu.ozu.mapp.system.DATA_REQUEST_PAYLOAD_WORLD_MOVE;
import edu.ozu.mapp.system.WorldOverseer;
import edu.ozu.mapp.utils.JSONWorldWatch;
import edu.ozu.mapp.utils.Point;
import org.springframework.util.Assert;

import javax.swing.*;
import java.util.ArrayList;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

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

    // SCENARIO CANVAS HELPER FUNCTIONS
    public ArrayList<Point> GetAgentPlannedPath()
    {
        return handler.GetAgentPlannedPath();
    }

    public Point[] GetBroadcast()
    {
        return handler.GetBroadcast();
    }

    public int GetAgentRemainingPathLength()
    {
        return handler.GetRemainingPathLength();
    }

    public Point GetCurrentLocation()
    {
        return handler.GetAgent().POS;
    }

    public void leave()
    {
        handler.leave();
    }

    // Handler Hooks

    public void Join(WorldOverseer world)
    {
        world.Register(this);
    }

    public void WORLD_HANDLER_JOIN_HOOK()
    {
        handler.WORLD_HANDLER_JOIN_HOOK();
    }

    public String GetAgentName()
    {
        return handler.getAgentName();
    }

    public void UpdateState(JSONWorldWatch watch)
    {
        handler.UpdateState(watch);
    }

    public void SetJoinCallback(Function<DATA_REQUEST_PAYLOAD_WORLD_JOIN, String[]> callback)
    {
        handler.SetJoinCallback(callback);
    }

    public void SetOnCollisionCheckDoneCallback(BiFunction<String, String[], String> callback)
    {
        handler.SET_COLLISION_CHECK_DONE(callback);
    }

    public void SetGetNegotiationsHook(Function<String, String[]> function)
    {
        handler.SetGetNegotiationsHook(function);
    }

    public void SetJoinNegotiationSession(BiConsumer<String, AgentHandler> function)
    {
        handler.SET_NEGOTIATION_JOIN_SESSION(function);
    }

    public void SetNegotiatedCallback(BiConsumer<String, String> callback)
    {
        handler.SetNegotiatedCallback(callback);
    }

    public void VerifyNegotiations()
    {
        handler.VerifyNegotiations();
    }

    public void SetVerifyNegotiationsCallback(BiConsumer<String, Boolean> verify_negotiations_callback)
    {
        handler.SET_WORLD_OVERSEER_VERIFY_NEGOTIATIONS_CALLBACK(verify_negotiations_callback);
    }

    public void SetMoveCallback(BiConsumer<AgentHandler, DATA_REQUEST_PAYLOAD_WORLD_MOVE> callback)
    {
        handler.SetMoveCallback(callback);
    }

    public void SetLeaveHook(Consumer<AgentHandler> leave)
    {
        handler.SET_WORLD_OVERSEER_HOOK_LEAVE(leave);
    }

    public void SetLogHook(Consumer<String> log)
    {
        handler.SET_WORLD_OVERSEER_HOOK_LOG(log);
    }

    public void SetUpdateBroadcastHook(BiConsumer<String, String[]> update_broadcast_hook)
    {
        handler.SET_WORLD_OVERSEER_HOOK_UPDATE_BROADCAST(update_broadcast_hook);
    }

    public void SetInvalidateHook(Consumer<String> hook)
    {
        handler.SET_WORLD_OVERSEER_HOOK_INVALIDATE(hook);
    }
}
