/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.ozu.mapp.agent.client.world;

import edu.ozu.mapp.system.DATA_LOG_DISPLAY;
import edu.ozu.mapp.system.WorldOverseer;

import javax.swing.*;
import javax.swing.text.Document;
import java.awt.*;
import java.util.Iterator;

/**
 *
 * @author freedrone
 */
public class LogDisplayPane extends javax.swing.JTextPane
{
    private final WorldOverseer world_overseer;
    private final int offset = 1;
    private final int font_size = 14;
    private JScrollPane scroll_pane;

    /**
     * Creates new form LogDisplayPane
     */
    public LogDisplayPane()
    {
        world_overseer = WorldOverseer.getInstance();

        initComponents();

        setFont(new java.awt.Font("Ubuntu Mono", Font.PLAIN, font_size)); // NOI18N
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 400, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 300, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables

    public DATA_LOG_DISPLAY data = null;
    public void SetData(DATA_LOG_DISPLAY data)
    {
        this.data = data;

        write();

//        this.revalidate();
//        this.repaint();
    }

    private void write()
    {
        if (data == null) return;

        StringBuilder sb = new StringBuilder();

        data.world_data
            .keySet().stream().sorted()
            .forEach(key -> {
                sb.append(String.format("%-11s : %s\n", key, data.world_data.get(key)));
            });
        sb.append("-------------\n");

        Iterator<String> iterator = data.agent_to_point.keySet().iterator();
        //noinspection WhileLoopReplaceableByForEach
        while (iterator.hasNext()) {
            String agent_id = iterator.next();
            String[] _data = world_overseer.GetAgentData(agent_id);

            sb.append(String.format("%-15s POS: %5s TOKEN: %3s REMAINING_PATH_LEN: %S\n", agent_id, _data[0], _data[1], _data[2]));
        }
        sb.append("-------------\n");
//        for (Object[] item : data.world_log)
//        {
//            sb.append(String.format("%-23s %s\n", item[1].toString(), item[0].toString()));
//        }

        setText(sb.toString());

        // Scroll to bottom
//        this.setText(out.toString());
//        JScrollBar bar = this.scroll_pane.getVerticalScrollBar();
//        bar.setValue(bar.getMaximum());
    }

    private void append_text(String text)
    {
        Document document = getDocument();

        // Move the insertion point to the end
        setCaretPosition(document.getLength());

        // Insert text
        replaceSelection(text);
    }

    public void SetScrollPane (JScrollPane scrollPane)
    {
        this.scroll_pane = scrollPane;
    }
}
