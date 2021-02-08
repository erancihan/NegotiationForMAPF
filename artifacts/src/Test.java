import edu.ozu.mapp.agent.client.world.ScenarioManager;
import edu.ozu.mapp.config.AgentConfig;
import edu.ozu.mapp.config.WorldConfig;

public class Test {
    public static void main(String[] args) {
        ScenarioManager.main(args);
    }

    public void test()
    {
        ScenarioManager manager = new ScenarioManager(true);

        for (Object[] o : manager.getAgentClassesList())
        {
            System.out.printf("%s%n", o[0]);
        }
    }

    public void run_gui()
    {
        new ScenarioManager();
    }

    public void gen_cases()
    {
        int number_of_cases_to_generate = 1;
        String timestamp = String.valueOf(System.currentTimeMillis());

        WorldConfig world_config = new WorldConfig();
        world_config.world_id = timestamp;
        world_config.width    = 32;
        world_config.height   = 32;
        world_config.min_path_len = 1;
        world_config.max_path_len = 100000;
        world_config.min_distance_between_agents = 1;
        world_config.agent_count = -1;
        world_config.initial_token_c = 10;
        world_config.number_of_expected_conflicts = 2;
        world_config.instantiation_configuration = new Object[][]{
                {"mapp.agent.Hybrid", 40},
        };

        for (int i = 0; i < number_of_cases_to_generate; i++)
        {
            ScenarioManager manager = new ScenarioManager(true);

            world_config.world_id = timestamp + "-" + i;
            manager
                .generateScenario(world_config)
                .thenAccept(agentConfigs -> {
                    AgentConfig[] agent_config = agentConfigs.toArray(new AgentConfig[0]);

                    manager.SaveScenario(agent_config, world_config);
                });
        }
    }
}
