import ch.qos.logback.classic.Level;
import edu.ozu.mapp.agent.client.world.ScenarioManager;
import edu.ozu.mapp.config.AgentConfig;
import edu.ozu.mapp.config.WorldConfig;
import edu.ozu.mapp.system.LeaveActionHandler;
import edu.ozu.mapp.system.SystemExit;
import edu.ozu.mapp.utils.Glob;
import edu.ozu.mapp.utils.Globals;
import edu.ozu.mapp.utils.TournamentRunner;

import javax.swing.filechooser.FileSystemView;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public class Experiment {
    public static void main(String[] args) {
        try {
            ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
            root.setLevel(Level.INFO);

            System.out.println(FileSystemView.getFileSystemView().getDefaultDirectory().getPath());
            System.out.println(System.getProperty("user.dir"));

            Path path = Paths.get(System.getProperty("user.dir"), "scenarios", "HybridAgent", "16x16_60");
            // Path path = Paths.get(System.getProperty("user.dir"), "scenarios", "RandomAgent", "16x16_60");
            ArrayList<String> scenarios = new Glob().glob(path, "world-scenario-*.json");

// SET FIELD OF VIEW
            Globals.FIELD_OF_VIEW_SIZE  = 7;     // d = 3 from current location to sides
            Globals.BROADCAST_SIZE      = 7;     // 2d + 1

// SET LEAVE ACTION BEHAVIOUR
            Globals.LEAVE_ACTION_BEHAVIOUR = LeaveActionHandler.LeaveActionTYPE.OBSTACLE;       // obstacle on exit
            // Globals.LEAVE_ACTION_BEHAVIOUR = LeaveActionHandler.LeaveActionTYPE.PASS_THROUGH; //    leave on exit

// SET MOVE ACTION SPACE SIZE
            Globals.MOVE_ACTION_SPACE_SIZE = 4; //   no wait action
            // Globals.MOVE_ACTION_SPACE_SIZE = 5; // with wait action

// SET BID SEARCH SPACE OVERRIDE
            Globals.BID_SEARCH_STRATEGY_OVERRIDE = edu.ozu.mapp.utils.bid.BidSpace.SearchStrategy.NO_DEPTH_LIMIT;

            Globals.MAX_BID_SPACE_POOL_SIZE = 100;

// TOURNAMENT CONFIG
            TournamentRunner.TOURNAMENT_RUNNER_MAX_NUMBER_OF_TRIES = 2; // retry once
            SystemExit.SHUTDOWN_ON_EXIT = false;

            File tournament_run_results = Paths.get(path.toString(), "runs.txt").toFile();
            //noinspection ResultOfMethodCallIgnored
            tournament_run_results.getParentFile().mkdirs();
            if (!tournament_run_results.exists()) {
                //noinspection ResultOfMethodCallIgnored
                tournament_run_results.createNewFile();
            }

            TournamentRunner runner = new TournamentRunner();
            runner.tournament_run_results = tournament_run_results;
            runner.run(scenarios);
        } catch (Exception exception) {
            exception.printStackTrace();
            System.exit(1);
        }
    }

    public void gen_cases()
    {
        int number_of_cases_to_generate = 1;
        String timestamp = String.valueOf(System.currentTimeMillis());

        for (int i = 0; i < number_of_cases_to_generate; i++)
        {
            WorldConfig world_config = new WorldConfig();
            world_config.world_id = timestamp;
            world_config.width    = 16;
            world_config.height   = 16;
            world_config.min_path_len = 8;
            world_config.max_path_len = 24;
            world_config.min_distance_between_agents = 1;
            world_config.agent_count = -1;
            world_config.initial_token_c = 10;
            world_config.number_of_expected_conflicts = 2;
            world_config.instantiation_configuration = new Object[][]{
                    {"mapp.agent.RandomAgent", 40},
            };

            ScenarioManager manager = new ScenarioManager(true);

            world_config.world_id = System.currentTimeMillis() + "-" + i;
            manager
                    .GenerateScenario(world_config)
                    .thenAccept(agentConfigs -> {
                        AgentConfig[] agent_config = agentConfigs.toArray(new AgentConfig[0]);

                        manager.SaveScenario(agent_config, world_config);
                    })
                    .exceptionally(ex -> { ex.printStackTrace(); return null; })
            ;
        }
    }
}
