import edu.ozu.mapp.agent.client.world.ScenarioManager;
import edu.ozu.mapp.config.AgentConfig;
import edu.ozu.mapp.config.WorldConfig;
import edu.ozu.mapp.utils.Glob;
import edu.ozu.mapp.utils.TournamentRunner;

import javax.swing.filechooser.FileSystemView;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public class Sample {
    public static void main(String[] args) {
        try {
            ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
            root.setLevel(ch.qos.logback.classic.Level.INFO);

            System.out.println(FileSystemView.getFileSystemView().getDefaultDirectory().getPath());
            System.out.println(System.getProperty("user.dir"));

///            ScenarioManager.run(args);
//            new Sample().gen_cases();
            new Sample().run_tournament();
        } catch (Exception e) {
            e.printStackTrace();
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

    File tournament_run_results;
    public void run_tournament() throws IOException, InterruptedException
    {
//        Globals.FIELD_OF_VIEW_SIZE = 5;

//        Path path = Paths.get(FileSystemView.getFileSystemView().getDefaultDirectory().getPath(), "MAPP");
        Path path = Paths.get(System.getProperty("user.dir"), "artifacts", "configs", "16x16_60-Hybrid_FoV5");
        ArrayList<String> scenarios = new Glob().glob(path, "world-scenario-*.json");

        tournament_run_results = Paths.get(path.toString(), "runs.txt").toFile();
        //noinspection ResultOfMethodCallIgnored
        tournament_run_results.getParentFile().mkdirs();
        if (!tournament_run_results.exists()) {
            //noinspection ResultOfMethodCallIgnored
            tournament_run_results.createNewFile();
        }

        TournamentRunner runner = new TournamentRunner();
        runner.tournament_run_results = tournament_run_results;
        runner.run(scenarios);
    }
}
