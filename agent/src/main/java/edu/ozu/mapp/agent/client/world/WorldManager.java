/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.ozu.mapp.agent.client.world;

import com.google.gson.Gson;
import edu.ozu.mapp.agent.client.WorldWatchSocketIO;
import edu.ozu.mapp.utils.Globals;
import edu.ozu.mapp.utils.Save;
import org.springframework.util.Assert;
import redis.clients.jedis.Jedis;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.util.stream.Collectors;

/**
 *
 * @author freedrone
 */
public class WorldManager extends javax.swing.JFrame {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(WorldManager.class);
    private Gson gson = new Gson();
    private WorldWatchSocketIO WorldListener = null;

    private String WID;
    private redis.clients.jedis.Jedis jedis;
    private boolean isJedisOK = true;
    private boolean loop = false;

    private World world;

    /**
     * Creates new form WorldHandler
     */
    public WorldManager()
    {
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
        java.awt.GridBagConstraints gridBagConstraints;

        cards_container = new javax.swing.JPanel();
        javax.swing.JPanel create = new javax.swing.JPanel();
        world_id = new javax.swing.JTextField();
        javax.swing.JButton create_btn = new javax.swing.JButton();
        javax.swing.JLabel jLabel1 = new javax.swing.JLabel();
        javax.swing.JPanel controller = new javax.swing.JPanel();
        javax.swing.JPanel text_view_container = new javax.swing.JPanel();
        javax.swing.JScrollPane jScrollPane2 = new javax.swing.JScrollPane();
        text_view = new javax.swing.JTextPane();
        javax.swing.JPanel controls_container = new javax.swing.JPanel();
        cycle_states_toggle_btn = new javax.swing.JToggleButton();
        javax.swing.JButton join_state_btn = new javax.swing.JButton();
        javax.swing.JButton broadcast_state_btn = new javax.swing.JButton();
        javax.swing.JButton negotiate_state_btn = new javax.swing.JButton();
        javax.swing.JButton move_state_btn = new javax.swing.JButton();
        javax.swing.JLabel jLabel4 = new javax.swing.JLabel();
        javax.swing.JButton save_logs_btn = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        cards_container.setMinimumSize(new java.awt.Dimension(400, 300));
        cards_container.setName(""); // NOI18N
        cards_container.setOpaque(false);
        cards_container.setLayout(new java.awt.CardLayout());

        create.setMinimumSize(new java.awt.Dimension(600, 88));
        create.setPreferredSize(new java.awt.Dimension(600, 300));
        java.awt.GridBagLayout createLayout = new java.awt.GridBagLayout();
        createLayout.columnWidths = new int[] {0, 5, 0, 5, 0};
        createLayout.rowHeights = new int[] {0, 5, 0, 5, 0};
        create.setLayout(createLayout);

        world_id.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        world_id.setText("world_id");
        world_id.setToolTipText("");
        world_id.setMinimumSize(new java.awt.Dimension(200, 37));
        world_id.setPreferredSize(new java.awt.Dimension(200, 40));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new java.awt.Insets(3, 3, 3, 3);
        create.add(world_id, gridBagConstraints);

        create_btn.setText("CREATE");
        create_btn.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        create_btn.setIconTextGap(10);
        create_btn.setPreferredSize(new java.awt.Dimension(100, 35));
        create_btn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                create_btnActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 4;
        create.add(create_btn, gridBagConstraints);

        jLabel1.setText("World ID");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        create.add(jLabel1, gridBagConstraints);

        cards_container.add(create, "create");

        controller.setLayout(new java.awt.GridBagLayout());

        text_view_container.setMinimumSize(new java.awt.Dimension(450, 300));
        text_view_container.setPreferredSize(new java.awt.Dimension(450, 300));
        text_view_container.setLayout(new java.awt.GridLayout(1, 0));

        jScrollPane2.setViewportBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));

        text_view.setEditable(false);
        text_view.setFont(new java.awt.Font("Monospaced", 0, 12)); // NOI18N
        jScrollPane2.setViewportView(text_view);

        text_view_container.add(jScrollPane2);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        controller.add(text_view_container, gridBagConstraints);

        controls_container.setPreferredSize(new java.awt.Dimension(150, 300));
        controls_container.setLayout(new java.awt.GridBagLayout());

        cycle_states_toggle_btn.setText("Cycle States");
        cycle_states_toggle_btn.setPreferredSize(new java.awt.Dimension(120, 31));
        cycle_states_toggle_btn.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                cycle_states_toggle_btnItemStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.insets = new java.awt.Insets(14, 0, 0, 0);
        controls_container.add(cycle_states_toggle_btn, gridBagConstraints);

        join_state_btn.setText("JOIN");
        join_state_btn.setPreferredSize(new java.awt.Dimension(120, 31));
        join_state_btn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                join_state_btnActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        controls_container.add(join_state_btn, gridBagConstraints);

        broadcast_state_btn.setText("BROADCAST");
        broadcast_state_btn.setPreferredSize(new java.awt.Dimension(120, 31));
        broadcast_state_btn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                broadcast_state_btnActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        controls_container.add(broadcast_state_btn, gridBagConstraints);

        negotiate_state_btn.setText("NEGOTIATE");
        negotiate_state_btn.setPreferredSize(new java.awt.Dimension(120, 31));
        negotiate_state_btn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                negotiate_state_btnActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        controls_container.add(negotiate_state_btn, gridBagConstraints);

        move_state_btn.setText("MOVE");
        move_state_btn.setPreferredSize(new java.awt.Dimension(120, 31));
        move_state_btn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                move_state_btnActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        controls_container.add(move_state_btn, gridBagConstraints);

        jLabel4.setText("States");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        controls_container.add(jLabel4, gridBagConstraints);

        save_logs_btn.setText("Save Logs");
        save_logs_btn.setPreferredSize(new java.awt.Dimension(120, 31));
        save_logs_btn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                save_logs_btnActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.insets = new java.awt.Insets(15, 0, 0, 0);
        controls_container.add(save_logs_btn, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        controller.add(controls_container, gridBagConstraints);

        cards_container.add(controller, "controller");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(cards_container, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(cards_container, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void create_btnActionPerformed(java.awt.event.ActionEvent event)
    {//GEN-FIRST:event_create_btnActionPerformed
        CardLayout cl = (CardLayout) cards_container.getLayout();
        cl.show(cards_container, "controller");

        create_world();
    }//GEN-LAST:event_create_btnActionPerformed

    private void formWindowClosing(java.awt.event.WindowEvent event)
    {//GEN-FIRST:event_formWindowClosing
        delete_world();
    }//GEN-LAST:event_formWindowClosing

    private void join_state_btnActionPerformed(java.awt.event.ActionEvent evt)
    {//GEN-FIRST:event_join_state_btnActionPerformed
        if (isJedisOK)
        {
            jedis.hset(WID, "world_state", "0");
        }
    }//GEN-LAST:event_join_state_btnActionPerformed

    private void broadcast_state_btnActionPerformed(java.awt.event.ActionEvent evt)
    {//GEN-FIRST:event_broadcast_state_btnActionPerformed
        if (isJedisOK)
        {
            jedis.hset(WID, "world_state", "1");
        }
    }//GEN-LAST:event_broadcast_state_btnActionPerformed

    private void negotiate_state_btnActionPerformed(java.awt.event.ActionEvent evt)
    {//GEN-FIRST:event_negotiate_state_btnActionPerformed
        if (isJedisOK)
        {
            jedis.hset(WID, "world_state", "2");
        }
    }//GEN-LAST:event_negotiate_state_btnActionPerformed

    private void move_state_btnActionPerformed(java.awt.event.ActionEvent evt)
    {//GEN-FIRST:event_move_state_btnActionPerformed
        if (isJedisOK)
        {
            jedis.hset(WID, "world_state", "3");
        }
    }//GEN-LAST:event_move_state_btnActionPerformed

    private void cycle_states_toggle_btnItemStateChanged(java.awt.event.ItemEvent evt)
    {//GEN-FIRST:event_cycle_states_toggle_btnItemStateChanged
        int state = evt.getStateChange();

        if (state == ItemEvent.SELECTED)
        {
            world.Loop();
            loop = true;
        }
        if (state == ItemEvent.DESELECTED)
        {
            world.Stop();
            loop = false;
        }
        logger.info("loop -> " + loop);
    }//GEN-LAST:event_cycle_states_toggle_btnItemStateChanged

    private void save_logs_btnActionPerformed(java.awt.event.ActionEvent evt)
    {//GEN-FIRST:event_save_logs_btnActionPerformed
        Save.stringToFile(text_view.getText(), world_id.getText() + "-world-log.txt");
    }//GEN-LAST:event_save_logs_btnActionPerformed

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
            java.util.logging.Logger.getLogger(WorldManager.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> new WorldManager().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel cards_container;
    private javax.swing.JToggleButton cycle_states_toggle_btn;
    private javax.swing.JTextPane text_view;
    private javax.swing.JTextField world_id;
    // End of variables declaration//GEN-END:variables

    private void onComponentsDidMount()
    {
        world_id.setText(String.valueOf(System.currentTimeMillis()));

        world = new World();
        world.SetOnLoopingStop(() -> cycle_states_toggle_btn.setSelected(false));

        jedis = new Jedis(Globals.REDIS_HOST);
        try {
            jedis.connect();
        } catch (Exception e) {
            logger.error("«can't connect to Redis»");
            isJedisOK = false;
            e.printStackTrace();
            // TODO pop a dialog window
        }
    }

    /**
     * Very first function that is invoked before all the
     * world functionality becomes available
     * */
    void create_world()
    {
        if (!isJedisOK) { return; }

        WID = "world:" + world_id.getText() + ":";
        WorldListener = world.Create(
            world_id.getText(),
            (data, log) -> {
                // update canvas
                try {
                    text_view.setText(
                        data
                            .keySet()
                            .stream()
                            .map(key -> key + ": " + data.get(key))
                            .collect(Collectors.joining("\n")) +
                        "\n-------------\n" +
                        log
                            .stream()
                            .map(item -> String.format("%-23s", item[1].toString()) + " " + item[0].toString())
                            .collect(Collectors.joining("\n"))
                    );
                } catch (Exception e) {
                    e.printStackTrace();
                    System.exit(1);
                }
            });

        Assert.notNull(WorldListener, "World Listener cannot be null!");
    }

    void delete_world()
    {
        if (!isJedisOK || WID == null) { return; }

        if (WorldListener != null) WorldListener.close();
        world.Delete();
    }
}
