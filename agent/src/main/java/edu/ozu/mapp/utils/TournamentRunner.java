package edu.ozu.mapp.utils;

import edu.ozu.mapp.agent.client.world.ScenarioManager;
import edu.ozu.mapp.config.AgentConfig;
import edu.ozu.mapp.config.WorldConfig;

import javax.swing.filechooser.FileSystemView;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.CountDownLatch;

public class TournamentRunner {
    public static void main(String[] arg) {
        try {
            System.out.println(FileSystemView.getFileSystemView().getDefaultDirectory().getPath());
            System.out.println(System.getProperty("user.dir"));

//            new TournamentRunner().gen_cases();
            new TournamentRunner().run_tournament();
        } catch (Exception exception) {
            exception.printStackTrace();
            System.exit(1);
        }
    }

    private void run_tournament() throws IOException
    {
//        Path path = Paths.get(FileSystemView.getFileSystemView().getDefaultDirectory().getPath(), "MAPP");
        Path path = Paths.get(System.getProperty("user.dir"), "artifacts", "configs");
        ArrayList<String> scenarios = new Glob().glob(path, "*.json");

        new TournamentRunner().run(scenarios);
    }

    private void gen_cases()
    {
        int number_of_cases_to_generate = 5;
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

    public void run(ArrayList<String> scenarios)
    {
        for (String path : scenarios) System.out.println("> " + path);

        Iterator<String> iterator = scenarios.iterator();
        while (iterator.hasNext())
        {
            CountDownLatch latch = new CountDownLatch(1);

            String scenario = iterator.next();
            System.out.println(scenario);
            ScenarioManager
                .run(new String[0])
                .thenApply(manager -> {
                    manager.OnWindowClosed(latch::countDown);

                    return manager.SetScenario(new File(scenario));
                })
                .thenApply(manager -> manager.RunScenario(true))
            ;

            try {
                latch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
                System.exit(1);
            }
        }

        System.exit(0);
    }
}
