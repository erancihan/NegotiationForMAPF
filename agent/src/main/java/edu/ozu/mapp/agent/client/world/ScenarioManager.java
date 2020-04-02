/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.ozu.mapp.agent.client.world;

import edu.ozu.mapp.agent.Agent;
import mappagent.sample.Conceder;
import mappagent.sample.Greedy;
import mappagent.sample.HelloAgent;
import edu.ozu.mapp.agent.client.AgentClient;
import edu.ozu.mapp.utils.Point;
import org.springframework.util.Assert;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 *
 * @author freedrone
 */
public class ScenarioManager extends javax.swing.JFrame
{
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ScenarioManager.class);
    private Random rng = new Random();

    private String worldID = null;

    /**
     * Creates new form ScenarioManager
     */
    public ScenarioManager()
    {
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
        javax.swing.JPanel create_scenario = new javax.swing.JPanel();
        javax.swing.JPanel panel_upper = new javax.swing.JPanel();
        javax.swing.JPanel inputs_container = new javax.swing.JPanel();
        javax.swing.JLabel label_width = new javax.swing.JLabel();
        width_input = new javax.swing.JTextField();
        javax.swing.JLabel label_height = new javax.swing.JLabel();
        height_input = new javax.swing.JTextField();
        javax.swing.Box.Filler filler1 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 10), new java.awt.Dimension(0, 10), new java.awt.Dimension(32767, 10));
        javax.swing.JButton run_scenario_btn = new javax.swing.JButton();
        javax.swing.JPanel scenario_info_container = new javax.swing.JPanel();
        javax.swing.JScrollPane jScrollPane1 = new javax.swing.JScrollPane();
        scenario_info_pane = new javax.swing.JTextPane();
        javax.swing.JPanel agent_list_container = new javax.swing.JPanel();
        javax.swing.JScrollPane jScrollPane2 = new javax.swing.JScrollPane();
        agents_table = new javax.swing.JTable();
        javax.swing.JPanel run_scenario = new javax.swing.JPanel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new java.awt.Dimension(600, 300));
        setSize(new java.awt.Dimension(600, 300));
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        cards_container.setLayout(new java.awt.CardLayout());

        create_scenario.setBackground(new java.awt.Color(230, 230, 230));
        create_scenario.setMaximumSize(new java.awt.Dimension(600, 300));
        create_scenario.setLayout(new java.awt.GridBagLayout());

        panel_upper.setLayout(new javax.swing.BoxLayout(panel_upper, javax.swing.BoxLayout.LINE_AXIS));

        inputs_container.setBackground(new java.awt.Color(240, 240, 240));
        inputs_container.setMaximumSize(new java.awt.Dimension(600, 300));
        inputs_container.setMinimumSize(new java.awt.Dimension(100, 26));
        inputs_container.setPreferredSize(new java.awt.Dimension(200, 100));
        inputs_container.setLayout(new java.awt.GridBagLayout());

        label_width.setText("Width");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        inputs_container.add(label_width, gridBagConstraints);

        width_input.setToolTipText("");
        width_input.setMinimumSize(new java.awt.Dimension(80, 26));
        width_input.setPreferredSize(new java.awt.Dimension(80, 26));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        inputs_container.add(width_input, gridBagConstraints);

        label_height.setText("Height");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        inputs_container.add(label_height, gridBagConstraints);

        height_input.setMinimumSize(new java.awt.Dimension(80, 26));
        height_input.setPreferredSize(new java.awt.Dimension(80, 26));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        inputs_container.add(height_input, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        inputs_container.add(filler1, gridBagConstraints);

        run_scenario_btn.setText("Generate");
        run_scenario_btn.setPreferredSize(new java.awt.Dimension(100, 30));
        run_scenario_btn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                run_scenario_btnActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        inputs_container.add(run_scenario_btn, gridBagConstraints);

        panel_upper.add(inputs_container);

        scenario_info_container.setPreferredSize(new java.awt.Dimension(400, 150));
        scenario_info_container.setLayout(new java.awt.BorderLayout());

        scenario_info_pane.setBackground(new java.awt.Color(250, 250, 250));
        jScrollPane1.setViewportView(scenario_info_pane);

        scenario_info_container.add(jScrollPane1, java.awt.BorderLayout.CENTER);

        panel_upper.add(scenario_info_container);

        create_scenario.add(panel_upper, new java.awt.GridBagConstraints());

        agent_list_container.setBackground(new java.awt.Color(238, 238, 230));
        agent_list_container.setPreferredSize(new java.awt.Dimension(600, 150));
        agent_list_container.setLayout(new java.awt.BorderLayout());

        agents_table.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jScrollPane2.setViewportView(agents_table);

        agent_list_container.add(jScrollPane2, java.awt.BorderLayout.CENTER);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        create_scenario.add(agent_list_container, gridBagConstraints);

        cards_container.add(create_scenario, "create");

        javax.swing.GroupLayout run_scenarioLayout = new javax.swing.GroupLayout(run_scenario);
        run_scenario.setLayout(run_scenarioLayout);
        run_scenarioLayout.setHorizontalGroup(
            run_scenarioLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 600, Short.MAX_VALUE)
        );
        run_scenarioLayout.setVerticalGroup(
            run_scenarioLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 300, Short.MAX_VALUE)
        );

        cards_container.add(run_scenario, "run");

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

    private void formWindowClosing(java.awt.event.WindowEvent evt)
    {//GEN-FIRST:event_formWindowClosing
        onClose();
    }//GEN-LAST:event_formWindowClosing

    private void run_scenario_btnActionPerformed(java.awt.event.ActionEvent evt)
    {//GEN-FIRST:event_run_scenario_btnActionPerformed
        // TODO add your handling code here:
        // switch card to run
//        CardLayout cl = (CardLayout) cards_container.getLayout();
//        cl.show(cards_container, "run");
        // start scenario
        generateScenario();
        runScenario();
    }//GEN-LAST:event_run_scenario_btnActionPerformed

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
            java.util.logging.Logger.getLogger(ScenarioManager.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> new ScenarioManager().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTable agents_table;
    private javax.swing.JPanel cards_container;
    private javax.swing.JTextField height_input;
    private javax.swing.JTextPane scenario_info_pane;
    private javax.swing.JTextField width_input;
    // End of variables declaration//GEN-END:variables

    private HashMap<String, Class<? extends Agent>> agents_map;
    private void onComponentsDidMount()
    {
        agents_map = new HashMap<>();
        // add agent classes by hand
        agents_map.put(HelloAgent.class.getSimpleName(), HelloAgent.class);
        agents_map.put(Greedy.class.getSimpleName(), Greedy.class);
        agents_map.put(Conceder.class.getSimpleName(), Conceder.class);

        AgentsTableModel table = new AgentsTableModel(agents_map.keySet().toArray(new String[0]));
        agents_table.setModel(table);
    }

    private void onClose()
    {
        if (listener == null || worldID == null)
        {
            return;
        }

        listener.close();
        WorldHandler.deleteWorld(worldID);
    }

    private RedisListener listener = null;
    private int agent_count = 0; // track number of agents there should be
    private void generateScenario()
    {
        // fetch scenario information
        int width = Integer.parseInt(width_input.getText());
        int height = Integer.parseInt(height_input.getText());
        int N = 1;

        // initialize world
        worldID = "world:" + System.currentTimeMillis() + ":";
        listener = WorldHandler.createWorld(worldID, (channel, message) -> {}); // TODO set bounds

        Assert.notNull(listener, "redis listener cannot be null!");
        listener.run();

        // initialize agents
        generateAgentStartLocations(width, height, N);
        generateAgentDestinations(width, height, N);
        initializeAgents();
    }

    private HashSet<String> AgentStartLocations = new HashSet<>();
    private void generateAgentStartLocations(int width, int height, int n)
    {
        for (int row = 0; row < agents_table.getRowCount(); row++)
        {   // for each row
            int ac = Integer.parseInt((String) agents_table.getValueAt(row, 1));
            for (int i = 0; i < ac; i++)
            {   // for the amount of agents that there is
                int x = -1;
                int y = -1;
                do {
                    x = rng.nextInt(width);
                    y = rng.nextInt(height);
                } while (!isPremisesClear(x, y, n));

                // assert x & y >= 0
                Assert.isTrue((x >= 0 && y >= 0), "P_t:(x, y) cannot be negative");
                // register to AgentStartLocations
                AgentStartLocations.add(x+":"+y);
            }
        }
    }

    private boolean isPremisesClear(int _x, int _y, int _N)
    {
        // TODO search the premises
        return true;
    }

    private HashSet<String> AgentDestinations = new HashSet<>();
    private void generateAgentDestinations(int width, int height, int n)
    {
        for (int row = 0; row < agents_table.getRowCount(); row++)
        {   // for each row
            int ac = Integer.parseInt((String) agents_table.getValueAt(row, 1));
            for (int i = 0; i < ac; i++)
            {   // for the amount of agents that there is
                int x = -1;
                int y = -1;
                do {
                    x = rng.nextInt(width);
                    y = rng.nextInt(height);
                } while (AgentDestinations.contains(x+":"+y));

                // assert x & y >= 0
                Assert.isTrue((x >= 0 && y >= 0), "P_d:(x, y) cannot be negative");
                // register to AgentDestinations
                AgentDestinations.add(x+":"+y);
            }
        }
    }

    private void initializeAgents()
    {
        Iterator<String> StartLocIter = AgentStartLocations.iterator();
        Iterator<String> DestIter = AgentDestinations.iterator();

        for (int row = 0; row < agents_table.getRowCount(); row++)
        {   // for each agent class
            String agentName = (String) agents_table.getValueAt(row, 0);
            int ac = Integer.parseInt((String) agents_table.getValueAt(row, 1));
            this.agent_count += ac;

            for (int i = 0; i < ac; i++)
            {   // for the amount of agents that there is
                logger.info("generating agent with ID: " + agentName + row + "" + i);

                Assert.isTrue(StartLocIter.hasNext() && DestIter.hasNext(), "Ran out of locations!");

                String[] StartLoc = StartLocIter.next().split(":");
                String[] DestLoc = DestIter.next().split(":");
                try {
                    AgentClient client = new AgentClient(
                            agents_map.get(agentName)
                            .getDeclaredConstructor(
                                    String.class,
                                    String.class,
                                    Point.class,
                                    Point.class
                            ).newInstance(
                                    "Agent" + row + "" + i,
                                    "Agent" + row + "" + i,
                                    new Point(StartLoc[0], StartLoc[1]), // randomise
                                    new Point(DestLoc[0], DestLoc[1])  // randomise
                            ));
                    client.join(worldID);
                } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                    e.printStackTrace();
                    System.exit(1);
                }
            }
        } // end loop over agents
    }

    private void runScenario()
    {
        // TODO start run scenario
    }

    private void redisListener(String channel, String message)
    {
        // will fire every time data is updated
        // or... just don't use this at all...
    }
}

class AgentsTableModel extends AbstractTableModel
{
    private boolean[][] editable_cells; // 2d array to represent rows and columns
    private String[] columns = new String[]{"Agent Class", "Count"};
    private ArrayList<Object[]> rows = new ArrayList<>();

    AgentsTableModel(String[] agents)
    {
        editable_cells = new boolean[agents.length][columns.length];

        for (int i = 0; i < agents.length; i++) {
            rows.add(new Object[]{agents[i], 0});
            editable_cells[i][0] = false;
            editable_cells[i][1] = true;
        }
    }

    @Override
    public Object getValueAt(int row, int col)
    {
        return rows.get(row)[col].toString();
    }

    @Override
    public void setValueAt(Object o, int row, int col)
    {
        if (o instanceof String)
            rows.get(row)[col] = Integer.parseInt(String.valueOf(o));
        this.fireTableCellUpdated(row, col);
    }

    public void setCellEditable(int row, int col, boolean value)
    {
        this.rows.get(row)[col] = value;
        this.fireTableCellUpdated(row, col);
    }

    @Override
    public String getColumnName(int column) {
        return columns[column];
    }

    @Override
    public int getRowCount() {
        return rows.size();
    }

    @Override
    public int getColumnCount() {
        return columns.length;
    }

    @Override
    public boolean isCellEditable(int row, int column)
    {
        return this.editable_cells[row][column];
    }
}