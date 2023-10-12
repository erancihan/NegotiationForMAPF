package edu.ozu.mapp.utils;

import com.google.gson.Gson;
import edu.ozu.mapp.agent.client.helpers.FileLogger;
import edu.ozu.mapp.agent.client.world.ScenarioManager;
import edu.ozu.mapp.config.AgentConfig;
import edu.ozu.mapp.config.WorldConfig;
import edu.ozu.mapp.system.LeaveActionHandler;
import edu.ozu.mapp.system.SystemExit;
import uk.org.lidalia.sysoutslf4j.context.SysOutOverSLF4J;

import javax.swing.filechooser.FileSystemView;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

public class TournamentRunner {
    public static boolean TOURNAMENT_RUNNER_RENAME_FILES_POST_RUN = true;
    public static int TOURNAMENT_RUNNER_MAX_NUMBER_OF_TRIES = 1;

    public File tournament_run_results;
    public Path homedir;

    public TournamentRunner()
    {
        SysOutOverSLF4J.sendSystemOutAndErrToSLF4J();

        homedir = Paths.get(FileSystemView.getFileSystemView().getDefaultDirectory().getPath(), "MAPP");
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
        Collections.sort(scenarios);
        for (String path : scenarios)
        {
            System.out.println("> " + path);
        }

        Iterator<String> scenario_iterator = scenarios.iterator();
        while (scenario_iterator.hasNext())
        {   // BEGIN: WHILE LOOP | while there are scenarios
            File scenario = new File(scenario_iterator.next()); System.out.println(scenario);

            String wid = scenario.getName().replace("world-scenario-", "").replace(".json", "");

            // BEGIN: check if it is on RUN CHECK file
            Path scenario_data_file_path = Paths.get(
                    scenario.getParent(),
                    String.format(
                            ".%s-fov%s-act%s-%s.txt",
                            wid.split("-")[0],
                            Globals.FIELD_OF_VIEW_SIZE,
                            Globals.MOVE_ACTION_SPACE_SIZE,
                            Globals.LEAVE_ACTION_BEHAVIOUR == LeaveActionHandler.LeaveActionTYPE.OBSTACLE
                                    ? "OBSTACLE"
                                    : "LEAVE"
                    )
            );

            //noinspection ResultOfMethodCallIgnored
            new File(String.valueOf(scenario_data_file_path)).createNewFile();

            System.out.printf("writing to %s%s", scenario_data_file_path, System.lineSeparator());

            if (Files.lines(scenario_data_file_path).anyMatch(line -> line.trim().equals(scenario.getName()))) {
                continue;
            }
            // END

            String __logfolder_path = String.valueOf(Paths.get(
                    String.valueOf(homedir),
                    "logs",
                    String.format(
                            "FoV%s_ACT%s_%s",
                            Globals.FIELD_OF_VIEW_SIZE,
                            Globals.MOVE_ACTION_SPACE_SIZE,
                            Globals.LEAVE_ACTION_BEHAVIOUR == LeaveActionHandler.LeaveActionTYPE.OBSTACLE ? "OBSTACLE" : "LEAVE"
                    ),
                    scenario_data_file_path.getParent().getParent().getFileName().toString(),
                    scenario_data_file_path.getParent().getFileName().toString()
                ));

            //noinspection ResultOfMethodCallIgnored
            new File(__logfolder_path).mkdirs();

            FileLogger.LOG_FOLDER = __logfolder_path;

            System.out.println("logging to folder: " + FileLogger.LOG_FOLDER);

            for (int run_idx = 1; run_idx <= TOURNAMENT_RUNNER_MAX_NUMBER_OF_TRIES; run_idx++)
            {   // BEGIN: FOR LOOP | number of retries
                AtomicBoolean scenario_success = new AtomicBoolean(false);

                File save_file_src = Paths.get(__logfolder_path, "WORLD-" + wid + "-" + run_idx).toFile();
                if (save_file_src.exists() || new File(save_file_src + "-true").exists() || new File(save_file_src + "-false").exists())
                {
                    continue;
                }
                //noinspection ResultOfMethodCallIgnored
                save_file_src.setWritable(true);

                String[] argv = new String[]{ String.valueOf(run_idx), "headless" };

                CountDownLatch latch = new CountDownLatch(1);

                // set system exit handling on simulation/scenario fail
                SystemExit.hook(status -> {
                    if (status == 0) {
                        return;
                    }
                    scenario_success.set(false);
                    latch.countDown();
                });

                CompletableFuture<ScenarioManager> future = ScenarioManager
                        .run(argv)
                        .thenApply(manager -> {
                            manager.OnWindowClosed(() -> {
                                scenario_success.set(true);
                                latch.countDown();

                                if (manager.world != null) {
                                    manager.world.kill();
                                }
                            });
                            manager.BindRunCrashHook();

                            return manager.SetScenario(scenario);
                        })
                        .thenApply(manager -> manager.RunScenario(true))
                        .exceptionally(throwable -> {
                            throwable.printStackTrace();
                            return null;
                        })
                ;

                // wait for simulation to terminate
                latch.await();

                System.out.println(">> instance done");

                future.join();
                System.gc();

                FileLogger __logger = FileLogger.getInstance();

                while (!__logger.queue_isEmpty()) {
                    System.out.println("write queue size::"+__logger.queue_len());
                    Thread.sleep(10000);
                    __logger.queue_work();
                }

                File save_file_dest = new File(save_file_src.getParent(), save_file_src.getName() + "-" + scenario_success.get());

                int __rename_retries = 0;
                do {
                    try {
                        Thread.sleep(1000);
                        Files.move(save_file_src.toPath(), save_file_dest.toPath());    // rename

                        __rename_retries += 10;
                    } catch (AccessDeniedException exception) {
                        exception.printStackTrace();
                        __rename_retries += 1;
                    }
                } while (__rename_retries < 10);

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
            }   // END: FOR LOOP

            if (TOURNAMENT_RUNNER_RENAME_FILES_POST_RUN)
            {
                new FileWriter(String.valueOf(scenario_data_file_path), true)
                        .append(String.format("%s%s", scenario.getName(), System.lineSeparator()))
                        .close()
                ;
            }
        }   // END: WHILE LOOP

        System.out.println("> Simulation over");
    }   // END: FUNCTION
}
