# How to use MAPP.jar

## Running Scenario Manager interface
Proper way to run `ScenarioManager` is to call the `run` _static_ member function of it.<br>
```java
class Sample {
    public static void main(String[] args) {
        ScenarioManager.run(args);
    }
}
```

## Creating Agents
A sample agent instantiation can be seen in the below code snippet.<br>
Class has to have `@MAPPAgent` annotation, must extend `Agent` abstract class and must be in `mapp.agent` package.<br>
Once they are set, rest can be automatically imported through your IDE.<br>
Otherwise import them from `edu.ozu.mapp` scope.

```java
package mapp.agent;

import ...

@edu.ozu.mapp.agent.MAPPAgent
public class AgentSample extends edu.ozu.mapp.agent.Agent {
    // System uses the following constructor. Any other constructor will fail 
    public AgentTest(String agentName, String agentID, Point start, Point dest, int initial_tokes) {
        super(agentName, agentID, start, dest, initial_tokes);
    }

    @Override
    public Action onMakeAction(Contract contract) {
        return null;
    }
}
```

## Generating Scenario Configurations
```java
class Sample {
    public void gen_cases() {
        int number_of_cases_to_generate = 1;
        String timestamp = String.valueOf(System.currentTimeMillis());

        WorldConfig world_config = new WorldConfig();
        world_config.world_id = timestamp;  // can be empty, placeholder for init 
        world_config.width = 32;
        world_config.height = 32;
        world_config.min_path_len = 1;
        world_config.max_path_len = 100000;
        world_config.min_distance_between_agents = 1;
        world_config.agent_count = -1;
        world_config.initial_token_c = 10;
        world_config.number_of_expected_conflicts = 2;
        world_config.instantiation_configuration = new Object[][]{
            // add agent full name and number of instances to configure for
            {"mapp.agent.RandomAgent", 40},
        };

        for (int i = 0; i < number_of_cases_to_generate; i++) {
            // ScenarioManager constructor takes a boolean value to determine
            //  whether to run in headless mode or not. "is_headless"
            // Setting this false will initialize GUI components.
            ScenarioManager manager = new ScenarioManager(true);

            // Update world_id to accommodate sub configurations on same timestamp
            //  can use "String.valueOf(System.currentTimeMillis());" instead as well.
            world_config.world_id = timestamp + "-" + i;
            manager
                .GenerateScenario(world_config) // ASYNC function, returns Promise
                .thenAccept(agentConfigs -> {
                    AgentConfig[] agent_config = agentConfigs.toArray(new AgentConfig[0]);

                    manager.SaveScenario(agent_config, world_config);
                });
        }
    }
}
```

## Running Tournament
To run a tournament setup, simply create an ArrayList of paths to scenario configurations, and provide it to TournamentRunner like in the example below.<br>
_Glob_ class used in the example is a helper class bundled in jar file.
```java
import edu.ozu.mapp.utils.TournamentRunner;

class Sample {
    public static void main(String[] args) {
        Path path = ...; // Path to directory where scenario configurations are
        ArrayList<String> scenarios = new Glob().glob(path, "*.json");

        new TournamentRunner().run(scenarios);
    }
}
```
Once the process is done, runner will exit on its own.
