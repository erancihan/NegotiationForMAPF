/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.ozu.mapp.agent.client.world;

import edu.ozu.mapp.dataTypes.WorldSnapshot;

import java.awt.event.ItemEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author Cihan Eran
 */
public class WorldHistoryPanel extends javax.swing.JPanel {

    /**
     * Creates new form WorldHistoryPanel
     */
    public WorldHistoryPanel()
    {
        history_store   = new ConcurrentHashMap<>();
        labels          = new ArrayList<>();

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

        dropdown = new javax.swing.JComboBox<>();
        canvas = new edu.ozu.mapp.agent.client.world.WorldHistoryCanvas();

        setMinimumSize(new java.awt.Dimension(100, 100));
        setLayout(new java.awt.BorderLayout());

        dropdown.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "- select -" }));
        dropdown.setToolTipText("");
        dropdown.setAutoscrolls(true);
        dropdown.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                dropdownItemStateChanged(evt);
            }
        });
        add(dropdown, java.awt.BorderLayout.PAGE_END);

        javax.swing.GroupLayout canvasLayout = new javax.swing.GroupLayout(canvas);
        canvas.setLayout(canvasLayout);
        canvasLayout.setHorizontalGroup(
            canvasLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 303, Short.MAX_VALUE)
        );
        canvasLayout.setVerticalGroup(
            canvasLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 329, Short.MAX_VALUE)
        );

        add(canvas, java.awt.BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents

    private void dropdownItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_dropdownItemStateChanged
        if (evt.getStateChange() == ItemEvent.SELECTED)
        {
            String item = (String) evt.getItem();
            display_snapshot(item);
        }
    }//GEN-LAST:event_dropdownItemStateChanged

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private edu.ozu.mapp.agent.client.world.WorldHistoryCanvas canvas;
    private javax.swing.JComboBox<String> dropdown;
    // End of variables declaration//GEN-END:variables

    private ConcurrentHashMap<String, WorldSnapshot>    history_store;
    private ArrayList<String>                           labels;

    public synchronized void SetSnapshot(String label, WorldSnapshot data)
    {
        if (!history_store.containsKey(label))
        {
            labels.add(label);
            history_store.put(label, data);

            dropdown.addItem(label);
        }
    }

    private void display_snapshot(String key)
    {
        if (!history_store.containsKey(key)) return;

        String[] agent_keys = history_store.get(key).agent_keys.toArray(new String[0]);
        Arrays.sort(agent_keys);

        canvas.snapshot = history_store.get(key);
        canvas.agent_keys = agent_keys;
        canvas.repaint();
    }
}
