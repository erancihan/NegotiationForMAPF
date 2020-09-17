/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.ozu.mapp.agent.client.world;

import edu.ozu.mapp.agent.Agent;
import edu.ozu.mapp.agent.MAPPAgent;
import edu.ozu.mapp.agent.client.AgentClient;
import edu.ozu.mapp.agent.client.WorldWatchSocketIO;
import edu.ozu.mapp.utils.Point;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.Assert;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;

/**
 *
 * @author freedrone
 */
public class ScenarioManager extends javax.swing.JFrame
{
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ScenarioManager.class);
    private Random rng = new Random();

    private String WorldID = null;

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
        javax.swing.JButton run_scenario_btn = new javax.swing.JButton();
        javax.swing.JPanel jPanel1 = new javax.swing.JPanel();
        path_length_input = new javax.swing.JTextField();
        javax.swing.JLabel label_width = new javax.swing.JLabel();
        height_input = new javax.swing.JTextField();
        width_input = new javax.swing.JTextField();
        javax.swing.JLabel jLabel1 = new javax.swing.JLabel();
        javax.swing.Box.Filler filler1 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 10), new java.awt.Dimension(0, 10), new java.awt.Dimension(32767, 10));
        javax.swing.JLabel label_height = new javax.swing.JLabel();
        javax.swing.Box.Filler filler2 = new javax.swing.Box.Filler(new java.awt.Dimension(5, 0), new java.awt.Dimension(5, 0), new java.awt.Dimension(5, 32767));
        min_dist_bw_agents = new javax.swing.JTextField();
        javax.swing.JLabel jLabel2 = new javax.swing.JLabel();
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

        inputs_container.setMaximumSize(new java.awt.Dimension(600, 300));
        inputs_container.setMinimumSize(new java.awt.Dimension(100, 26));
        inputs_container.setPreferredSize(new java.awt.Dimension(200, 100));
        inputs_container.setLayout(new java.awt.GridBagLayout());

        run_scenario_btn.setText("Generate");
        run_scenario_btn.setPreferredSize(new java.awt.Dimension(100, 30));
        run_scenario_btn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                run_scenario_btnActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        inputs_container.add(run_scenario_btn, gridBagConstraints);

        jPanel1.setLayout(new java.awt.GridBagLayout());

        path_length_input.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        path_length_input.setPreferredSize(new java.awt.Dimension(80, 26));
        path_length_input.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                path_length_inputActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 3;
        jPanel1.add(path_length_input, gridBagConstraints);

        label_width.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        label_width.setText("Width");
        label_width.setPreferredSize(new java.awt.Dimension(80, 14));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        jPanel1.add(label_width, gridBagConstraints);

        height_input.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        height_input.setMinimumSize(new java.awt.Dimension(80, 26));
        height_input.setPreferredSize(new java.awt.Dimension(80, 26));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        jPanel1.add(height_input, gridBagConstraints);

        width_input.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        width_input.setToolTipText("");
        width_input.setMinimumSize(new java.awt.Dimension(80, 26));
        width_input.setPreferredSize(new java.awt.Dimension(80, 26));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        jPanel1.add(width_input, gridBagConstraints);

        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel1.setText("Path Len. >=");
        jLabel1.setPreferredSize(new java.awt.Dimension(80, 14));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        jPanel1.add(jLabel1, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 5;
        jPanel1.add(filler1, gridBagConstraints);

        label_height.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        label_height.setText("Height");
        label_height.setPreferredSize(new java.awt.Dimension(80, 14));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        jPanel1.add(label_height, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        jPanel1.add(filler2, gridBagConstraints);

        min_dist_bw_agents.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        min_dist_bw_agents.setPreferredSize(new java.awt.Dimension(80, 26));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 4;
        jPanel1.add(min_dist_bw_agents, gridBagConstraints);

        jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel2.setText("min dist b/w");
        jLabel2.setPreferredSize(new java.awt.Dimension(80, 14));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        jPanel1.add(jLabel2, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        inputs_container.add(jPanel1, gridBagConstraints);

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
        GenerateScenario();
        RunScenario();
    }//GEN-LAST:event_run_scenario_btnActionPerformed

    private void path_length_inputActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_path_length_inputActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_path_length_inputActionPerformed

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
    private javax.swing.JTextField min_dist_bw_agents;
    private javax.swing.JTextField path_length_input;
    private javax.swing.JTextPane scenario_info_pane;
    private javax.swing.JTextField width_input;
    // End of variables declaration//GEN-END:variables

    private LinkedHashMap<String, Class<? extends Agent>> agents_map = new LinkedHashMap<>();
    private void onComponentsDidMount()
    {
        logger.debug("searching classes");
        FindClasses();
        logger.debug("classes found are");
        logger.debug(agents_map.toString());

        AgentsTableModel table = new AgentsTableModel(agents_map.keySet().toArray(new String[0]));
        agents_table.setModel(table);
    }

    private void FindClasses()
    {
        HashMap<String, Class<? extends Agent>> agents = new HashMap<>();

        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(true);
        scanner.addIncludeFilter(new AnnotationTypeFilter(MAPPAgent.class));

        try {
            for (BeanDefinition bd : scanner.findCandidateComponents("mappagent.sample")) {
                agents.put(
                        Objects.requireNonNull(bd.getBeanClassName()).split("\\.", 3)[2],
                        (Class<? extends Agent>) Class.forName(bd.getBeanClassName())
                );
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        agents_map = agents.entrySet().stream().sorted(Map.Entry.comparingByKey()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (o, n) -> o, LinkedHashMap::new));
    }

    private void onClose()
    {
        if (world_listener == null || WorldID == null)
        {
            return;
        }

        world_listener.close();
        World.Delete(WorldID);
    }

    private World world;
    private WorldWatchSocketIO world_listener = null;
    private int agent_count = 0; // track number of agents there should be
    private void GenerateScenario()
    {
        String wid = String.valueOf(System.currentTimeMillis());
        WorldID = "world:" + wid + ":";

        // fetch scenario information
        int width, height, min_path_len, min_d;
        try {
            width        = Integer.parseInt(width_input.getText());
            height       = Integer.parseInt(height_input.getText());
            min_path_len = path_length_input.getText().isEmpty() ? 0 : Integer.parseInt(path_length_input.getText());
            min_d        = min_dist_bw_agents.getText().isEmpty() ? 0 : Integer.parseInt(min_dist_bw_agents.getText());
        } catch (NumberFormatException ex) {
            logger.error("Encountered following error, stopping Scenario Generation");
            ex.printStackTrace();
            return;
        }

        // initialize agents
        GenerateAgentStartLocations(width, height, min_d);

        if (agent_count == 0) return;

        GenerateAgentLocationData(width, height, min_path_len);

        // initialize world
        world = new World();
        world.SetOnLoopingStop(() -> { });
        world_listener = world.Create(
            wid,
            width + "x" + height,
            (data, log) -> {
                // update canvas
                try {
                    scenario_info_pane.setText(
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

        Assert.notNull(world_listener, "redis listener cannot be null!");
        InitializeAgents();
    }

    private HashSet<String> AgentStartLocations = new HashSet<>();
    private HashSet<String> AgentDestinations = new HashSet<>();
    private ArrayList<Point[]> AgentLocationData = new ArrayList<>();

    private void GenerateAgentStartLocations(int width, int height, int min_d)
    {
        for (int row = 0; row < agents_table.getRowCount(); row++)
        {   // for each row in AGENTS table | foreach agent class present
            int ac = Integer.parseInt((String) agents_table.getValueAt(row, 1));
            for (int i = 0; i < ac; i++)
            {   // for the amount of agents that there is
                int x = -1;
                int y = -1;
                do {
                    x = rng.nextInt(width);
                    y = rng.nextInt(height);
                } while (!isPremisesClear(x, y, min_d) || AgentStartLocations.contains(x+":"+y));

                // assert x & y >= 0
                Assert.isTrue((x >= 0 && y >= 0), "P_t:(x, y) cannot be negative");
                // register to AgentStartLocations
                AgentStartLocations.add(x+":"+y);

                agent_count++;
            }
        }
    }

    private boolean isPremisesClear(int _x, int _y, int _d)
    {
        // search the premises
        for (int i = 0; i < _d; i++) {
            for (int j = 0; j < _d; j++) {
                if (i == 0 && j == 0) continue; // self

                boolean is_occupied =
                        AgentStartLocations.contains((_x + i) + ":" + (_y + j)) ||
                        AgentStartLocations.contains((_x + i) + ":" + (_y - j)) ||
                        AgentStartLocations.contains((_x - i) + ":" + (_y + j)) ||
                        AgentStartLocations.contains((_x - i) + ":" + (_y - j));

                if (is_occupied) {
                    // Premise NOT Clear
                    return false;
                }
            }
        }

        return true;
    }

    private void GenerateAgentLocationData(int width, int height, int n)
    {
        Iterator<String> StartLocIter = AgentStartLocations.iterator();
        while (StartLocIter.hasNext()) {
            // for each start point
            Point start = new Point(StartLocIter.next().split(":"));
            Point dest;
            do {
                dest = new Point(rng.nextInt(width), rng.nextInt(height));
            } while (AgentDestinations.contains(dest.key) || dest.ManhattanDistTo(start) < n);

            AgentLocationData.add(new Point[]{start, dest});
        }
    }

    private void InitializeAgents()
    {
        Iterator<Point[]> AgentLocationDataIterator = AgentLocationData.iterator();
        for (int row = 0; row < agents_table.getRowCount(); row++)
        {
            String agent_class_name = (String) agents_table.getValueAt(row, 0);
            int agent_count = Integer.parseInt((String) agents_table.getValueAt(row, 1));

            for (int i = 0; i < agent_count; i++)
            {
                if (!AgentLocationDataIterator.hasNext()) {
                    // error
                    logger.error("NOT ENOUGH LOCATIONS WERE GENERATED");
                    System.exit(1);
                }
                Point[] locPair = AgentLocationDataIterator.next();

                Point start = locPair[0];
                Point dest = locPair[1];

                logger.info("generating agent with ID: " + agent_class_name + row + "" + i + "|" + start + "->" + dest);

                try {
                    new AgentClient(
                            agents_map
                                    .get(agent_class_name)
                                    .getDeclaredConstructor(String.class, String.class, Point.class, Point.class)
                                    .newInstance("Agent" + row + "" + i, "Agent" + row + "" + i, start, dest)
                    ).join(WorldID);
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                    logger.error("An error occurred while trying to generate a client");
                    e.printStackTrace();
                    System.exit(1);
                }
            }
        }
    }

    private void RunScenario()
    {
        if (world != null) {
            world.Loop();
        }
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
