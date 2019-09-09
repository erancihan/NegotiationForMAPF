package edu.ozu.drone.client;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

@SpringBootApplication
public class Runner {
    private static String PORT = "8080";
    private static HashMap<String, AgentClient> agents = new HashMap<>();

    public static void main(String[] args) {
        try {
            System.setProperty("server.port", PORT);
            System.setProperty("spring.devtools.restart.enabled", "false");
            System.setProperty("spring.output.ansi.enabled", "ALWAYS");

            SpringApplication.run(Runner.class, args);

            init();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private static void init() throws ClassNotFoundException {
        Set<Class> agents = getRunTargets();

        agents.forEach(Runner::initAgent);
    }

    private static Set<Class> getRunTargets() throws ClassNotFoundException {
        Set<Class> classes = new HashSet<>();

        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(true);
        scanner.addIncludeFilter(new AnnotationTypeFilter(DroneAgent.class));

        for (BeanDefinition bd : scanner.findCandidateComponents("edu.ozu.drone.agent")) {
            Class<?> cl = Class.forName(bd.getBeanClassName());
            classes.add(cl);
        }

        return classes;
    }

    private static void initAgent(Class v) {
        try {
            AgentClient agent = (AgentClient) Class.forName(v.getName()).getConstructor().newInstance();
            agent.setPort(PORT);

            agents.put(agent.AGENT_ID, agent);

            Thread thread = new Thread(agent::run);
            thread.start();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }



    static void joinAgent(String agentId, String worldId) {
        agents.get(agentId).join(worldId);
    }
}
