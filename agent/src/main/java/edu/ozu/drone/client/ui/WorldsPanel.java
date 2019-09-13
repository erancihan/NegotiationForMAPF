/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.ozu.drone.client.ui;

import com.google.gson.Gson;
import edu.ozu.drone.utils.JSONWorldCreate;
import edu.ozu.drone.utils.JSONWorldsList;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 *
 * @author freedrone
 */
public class WorldsPanel extends javax.swing.JPanel {

    private String server;
    private String world_id = "";
    private String agent_name = "";
    private AgentUI parent;

    /**
     * Creates new form WorldsPanel
     */
    public WorldsPanel() {
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

        join_confirm = new javax.swing.JDialog();
        javax.swing.JPanel join_confirm_container = new javax.swing.JPanel();
        join_confirm_text = new javax.swing.JLabel();
        join_confirm_btn = new javax.swing.JButton();
        javax.swing.JPanel jPanel2 = new javax.swing.JPanel();
        new_world_btn = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        worlds_list = new javax.swing.JList<>();
        javax.swing.JPanel jPanel1 = new javax.swing.JPanel();
        javax.swing.JButton refresh_btn = new javax.swing.JButton();
        join_btn = new javax.swing.JButton();

        join_confirm.setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        join_confirm.setAlwaysOnTop(true);
        join_confirm.setResizable(false);
        join_confirm.setSize(new java.awt.Dimension(300, 150));
        join_confirm.getContentPane().setLayout(new java.awt.FlowLayout());

        join_confirm_container.setPreferredSize(new java.awt.Dimension(240, 100));
        java.awt.GridBagLayout jPanel3Layout = new java.awt.GridBagLayout();
        jPanel3Layout.columnWidths = new int[] {0, 35, 0, 35, 0};
        jPanel3Layout.rowHeights = new int[] {0, 15, 0};
        join_confirm_container.setLayout(jPanel3Layout);

        join_confirm_text.setText("Joining");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        join_confirm_container.add(join_confirm_text, gridBagConstraints);

        join_confirm_btn.setText("OK");
        join_confirm_btn.setMaximumSize(new java.awt.Dimension(100, 50));
        join_confirm_btn.setPreferredSize(new java.awt.Dimension(100, 35));
        join_confirm_btn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                join_confirm_btnActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        join_confirm_container.add(join_confirm_btn, gridBagConstraints);

        join_confirm.getContentPane().add(join_confirm_container);

        java.awt.FlowLayout flowLayout1 = new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 0, 0);
        flowLayout1.setAlignOnBaseline(true);
        setLayout(flowLayout1);

        jPanel2.setMinimumSize(new java.awt.Dimension(79, 40));
        jPanel2.setPreferredSize(new java.awt.Dimension(400, 40));

        new_world_btn.setText("CREATE NEW WORLD");
        new_world_btn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                new_world_btnActionPerformed(evt);
            }
        });
        jPanel2.add(new_world_btn);

        add(jPanel2);

        jScrollPane1.setPreferredSize(new java.awt.Dimension(400, 220));

        worlds_list.setModel(new javax.swing.AbstractListModel<String>() {
            String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };
            public int getSize() { return strings.length; }
            public String getElementAt(int i) { return strings[i]; }
        });
        worlds_list.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        worlds_list.setLayoutOrientation(javax.swing.JList.HORIZONTAL_WRAP);
        worlds_list.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                worlds_listValueChanged(evt);
            }
        });
        jScrollPane1.setViewportView(worlds_list);

        add(jScrollPane1);

        jPanel1.setMinimumSize(new java.awt.Dimension(79, 40));
        jPanel1.setPreferredSize(new java.awt.Dimension(400, 40));
        java.awt.GridBagLayout jPanel1Layout = new java.awt.GridBagLayout();
        jPanel1Layout.columnWidths = new int[] {0, 30, 0};
        jPanel1Layout.rowHeights = new int[] {0};
        jPanel1.setLayout(jPanel1Layout);

        refresh_btn.setText("REFRESH");
        refresh_btn.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        refresh_btn.setPreferredSize(new java.awt.Dimension(100, 31));
        refresh_btn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                refresh_btnActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        jPanel1.add(refresh_btn, gridBagConstraints);

        join_btn.setText("JOIN");
        join_btn.setPreferredSize(new java.awt.Dimension(100, 31));
        join_btn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                join_btnActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        jPanel1.add(join_btn, gridBagConstraints);

        add(jPanel1);
    }// </editor-fold>//GEN-END:initComponents

    void setAgentName(String agent_name) {
        this.agent_name = agent_name;
    }

    void setServer(String server) {
        this.server = server;
    }

    void setParent(AgentUI ui) {
        this.parent = ui;
    }

    private void join_btnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_join_btnActionPerformed
        if (!world_id.isEmpty())
        {
            join();
        }
    }//GEN-LAST:event_join_btnActionPerformed

    private void refresh_btnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_refresh_btnActionPerformed
        getWorldList();
    }//GEN-LAST:event_refresh_btnActionPerformed

    private void new_world_btnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_new_world_btnActionPerformed
        postWorldCreate();
    }//GEN-LAST:event_new_world_btnActionPerformed

    private void worlds_listValueChanged(javax.swing.event.ListSelectionEvent event) {//GEN-FIRST:event_worlds_listValueChanged
        if (!event.getValueIsAdjusting())
        {
            ListSelectionModel lsm = ((javax.swing.JList) event.getSource()).getSelectionModel();
            if (lsm.isSelectionEmpty())
            {
                world_id = "";
                join_btn.setEnabled(false);
            } else {
                world_id =  worlds_list.getSelectedValue();
                join_btn.setEnabled(true);
            }
        }
    }//GEN-LAST:event_worlds_listValueChanged

    private void join_confirm_btnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_join_confirm_btnActionPerformed
        join_confirm.setVisible(false);
        parent.join(world_id);
        join_confirm.dispose();
    }//GEN-LAST:event_join_confirm_btnActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JButton join_btn;
    private javax.swing.JDialog join_confirm;
    private javax.swing.JButton join_confirm_btn;
    private javax.swing.JLabel join_confirm_text;
    private javax.swing.JButton new_world_btn;
    private javax.swing.JList<String> worlds_list;
    // End of variables declaration//GEN-END:variables

    private void onComponentsDidMount() {
        if (world_id.isEmpty())
        {
            join_btn.setEnabled(false);
        }
    }

    private void getWorldList() {
        // fetch worlds list
        try {
            URL url = new URL("http://" + this.server + "/worlds");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("GET");

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));

            String il;
            StringBuffer response = new StringBuffer();
            while ((il = in.readLine()) != null)
            {
                response.append(il);
            }

            Gson gson = new Gson();
            JSONWorldsList wl = gson.fromJson(String.valueOf(response), JSONWorldsList.class);
            worlds_list.setListData(wl.getWorlds());
//            System.out.println("worlds:" + Arrays.toString(wl.getWorlds()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("Duplicates")
    private void postWorldCreate() {
        String wid = String.valueOf(System.currentTimeMillis());
        try {
            URL url = new URL("http://" + this.server + "/world/create");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; utf-8");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);

            String post_data = "{\"world_id\": \""+ wid + "\"}";

            // write to output stream
            try (OutputStream stream = conn.getOutputStream())
            {
                byte[] bytes = post_data.getBytes(StandardCharsets.UTF_8);
                stream.write(bytes, 0, bytes.length);
            }

            // response
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            String il;
            StringBuilder response = new StringBuilder();
            while ((il = in.readLine()) != null)
            {
                response.append(il);
            }
            // refresh list
            getWorldList();

            // todo success
            Gson gson = new Gson();
            JSONWorldCreate wc = gson.fromJson(String.valueOf(response), JSONWorldCreate.class);
            this.world_id = wc.getWorld_id();

            System.out.println("> create world response: " + wc);

            join();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void join() {
        join_confirm.setVisible(true);
        join_confirm.setTitle(agent_name);
        join_confirm_text.setText("Joining to \n<" + world_id + ">");
    }

    void loadList() {
        getWorldList();
    }
}
