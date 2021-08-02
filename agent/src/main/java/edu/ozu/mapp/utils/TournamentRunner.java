package edu.ozu.mapp.utils;

import com.google.gson.Gson;
import edu.ozu.mapp.agent.client.world.ScenarioManager;
import edu.ozu.mapp.config.AgentConfig;
import edu.ozu.mapp.config.WorldConfig;
import edu.ozu.mapp.system.SystemExit;
import uk.org.lidalia.sysoutslf4j.context.SysOutOverSLF4J;

import javax.swing.filechooser.FileSystemView;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

public class TournamentRunner {
    public static boolean TOURNAMENT_RUNNER_RENAME_FILES_POST_RUN = true;
    public static int TOURNAMENT_RUNNER_MAX_NUMBER_OF_TRIES = 1;

    public File tournament_run_results;

    public TournamentRunner()
    {
        SysOutOverSLF4J.sendSystemOutAndErrToSLF4J();
    }

    private void run_tournament() throws IOException, InterruptedException
    {
        Globals.FIELD_OF_VIEW_SIZE = 5;

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

        run(scenarios);
    }

    @SuppressWarnings("DuplicatedCode")
    private void fetch_cbs_conf() throws IOException, InterruptedException
    {
        Gson gson = new Gson();

        Path path = Paths.get(System.getProperty("user.dir"), "artifacts", "configs", "8x8_25-cbs");
        ArrayList<String> confs = new Glob().glob(path, "\\**\\*.json");

        String timestamp = String.valueOf(System.currentTimeMillis());
        for (String config_path : confs)
        {
            Path __path = Paths.get(config_path);

            String __data = __path.getFileName().toString().split("\\.")[0];
            String[] __idx = __data.split("-");
            String idx = __idx[__idx.length-1];

            StringBuilder sb = new StringBuilder();
            try (Stream<String> stream = Files.lines(__path, StandardCharsets.UTF_8)) {
                stream.forEach(s -> sb.append(s).append("\n"));
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(500);
            }
            CBSConfig config = gson.fromJson(sb.toString(), CBSConfig.class);

            WorldConfig world_config = new WorldConfig();
            world_config.world_id = timestamp + "-" + "8x8_25-" + idx;
            world_config.width = config.map.get("dimensions").get(0);
            world_config.height = config.map.get("dimensions").get(1);
            world_config.min_path_len = 1;
            world_config.max_path_len = 100000;
            world_config.min_distance_between_agents = 1;
            world_config.agent_count = -1;
            world_config.initial_token_c = 10;
            world_config.number_of_expected_conflicts = 0;
            world_config.instantiation_configuration = new Object[][]{
                    {"mapp.agent.HybridAgent", config.agents.size()},
            };

            ScenarioManager manager = new ScenarioManager(true);
            CountDownLatch __latch = new CountDownLatch(1);
            manager
                .GenerateScenario(world_config)
                .thenAccept(agentConfigs -> {
                    AgentConfig[] agent_config = agentConfigs.toArray(new AgentConfig[0]);
                    for (int i = 0; i < agent_config.length; i++) {
                        ArrayList<Double> start = (ArrayList<Double>) config.agents.get(i).get("start");
                        agent_config[i].start.x = start.get(0).intValue();
                        agent_config[i].start.y = start.get(1).intValue();

                        ArrayList<Double> dest = (ArrayList<Double>) config.agents.get(i).get("goal");
                        agent_config[i].dest.x = dest.get(0).intValue();
                        agent_config[i].dest.y = dest.get(1).intValue();
                    }

                    manager.SaveScenario(agent_config, world_config);
                    __latch.countDown();
                })
                .exceptionally(ex -> {
                    ex.printStackTrace();
                    return null;
                })
            ;
            __latch.await();
        }
    }

    private void gen_cases() throws InterruptedException
    {
        int number_of_cases_to_generate = 1;

        for (int i = 0; i < number_of_cases_to_generate; i++)
        {
            String timestamp = String.valueOf(System.currentTimeMillis());

            WorldConfig world_config = new WorldConfig();
            world_config.world_id = timestamp + "-" + i;
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
            CountDownLatch __latch = new CountDownLatch(1);
            manager
                .GenerateScenario(world_config)
                .thenAccept(agentConfigs -> {
                    AgentConfig[] agent_config = agentConfigs.toArray(new AgentConfig[0]);

                    manager.SaveScenario(agent_config, world_config);
                    __latch.countDown();
                })
                .exceptionally(ex -> { ex.printStackTrace(); return null; })
            ;
            __latch.await();
        }
    }

    public void run(ArrayList<String> scenarios) throws InterruptedException, IOException
    {   // BEGIN: FUNCTION
        for (String path : scenarios) {
            System.out.println("> " + path);
        }

        Iterator<String> iterator = scenarios.iterator();
        while (iterator.hasNext())
        {   // BEGIN: WHILE LOOP | while there are scenarios
            File scenario = new File(iterator.next());
            System.out.println(scenario);

            String wid = scenario.getName().replace("world-scenario-", "").replace(".json", "");


            int try_count = 1;
            for (int i = 0; i < try_count && try_count <= TOURNAMENT_RUNNER_MAX_NUMBER_OF_TRIES; i++)
            {   // BEGIN: FOR LOOP | number of retries
                AtomicBoolean should_repeat = new AtomicBoolean(false);

                File save_file_src;
                int idx = i;
                do {
                    idx++;
                    save_file_src = Paths.get(FileSystemView.getFileSystemView().getDefaultDirectory().getPath(), "MAPP", "logs", "WORLD-" + wid + "-" + idx).toFile();
                } while (new File(save_file_src + "-false").exists() || new File(save_file_src + "-true").exists() || save_file_src.exists());

                String[] argv = new String[]{ String.valueOf(idx) };

                CountDownLatch latch = new CountDownLatch(1);

                // set system exit handling on simulation/scenario fail
                SystemExit.hook(status -> {
                    if (status == 0)
                        return;

                    should_repeat.set(true);
                    latch.countDown();
                });

                ScenarioManager
                    .run(argv)
                    .thenApply(manager -> {
                        manager.OnWindowClosed(latch::countDown);
                        manager.BindRunCrashHook();

                        return manager.SetScenario(scenario);
                    })
                    .thenApply(manager -> manager.RunScenario(true))
                ;

                // wait for simulation to terminate
                latch.await();

                System.out.println(">> instance done");
                Thread.sleep(10000);

                File save_file_dest = new File(save_file_src.getParent(), save_file_src.getName() + "-" + !should_repeat.get());
                Files.move(save_file_src.toPath(), save_file_dest.toPath());

                if (tournament_run_results != null)
                {
                    new FileWriter(tournament_run_results, true)
                        .append(
                            String.format(
                                "%s;%s%s",
                                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date()),
                                save_file_dest.getName(),
                                System.lineSeparator()
                            )
                        )
                        .close();
                }

                if (should_repeat.get())
                {
                    try_count += 1;
                }
            }   // END: FOR LOOP

            if (TOURNAMENT_RUNNER_RENAME_FILES_POST_RUN)
            {
                File __new = new File(scenario.getParent(), "__" + scenario.getName());
                Files.move(scenario.toPath(), __new.toPath());
            }
        }   // END: WHILE LOOP

        System.out.println("> Simulation over");
        System.exit(0);
    }   // END: FUNCTION
}
