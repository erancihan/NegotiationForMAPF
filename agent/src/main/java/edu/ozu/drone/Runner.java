package edu.ozu.drone;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
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
            AgentClient x = (AgentClient) Class.forName(v.getName()).getConstructor().newInstance();
            x.__init(PORT);
            x.__setAgentAtController();

            agents.put(x.AGENT_ID, x);

            x.__launchBrowser();

            Thread thread = new Thread(x::run);
            thread.start();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }



    static void joinAgent(String agentId, String worldId) {
        agents.get(agentId).join(worldId);
    }
}
