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
import java.util.HashSet;
import java.util.Set;

@SpringBootApplication
public class Runner {
    private static String PORT = "8080";
    private static Set<AgentClient> agents = new HashSet<>();

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
            x.__init();

            assert !x.AGENT_ID.isEmpty();
            assert x.START != null;

            setAgentAtController(x);
            agents.put(x.AGENT_ID, x);

            launchBrowser(x.AGENT_ID);

            Thread thread = new Thread(x::run);
            thread.start();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("Duplicates")
    private static void setAgentAtController(AgentClient agent) {
        System.out.println("> passing agent data to backend controller");
        try {
            URL url = new URL("http://localhost:" + PORT + "/set-agent");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json; utf-8");
            con.setRequestProperty("Accept", "application/json");
            con.setDoOutput(true);

            String jsonInputString = "{" +
                    "\"id\": \"" + agent.AGENT_ID + "\"," +
                    "\"x\": \""  + agent.START.x  + "\"," +
                    "\"y\": \""  + agent.START.y  + "\" }";

            try (OutputStream outs = con.getOutputStream())
            {
                byte[] inb = jsonInputString.getBytes(StandardCharsets.UTF_8);
                outs.write(inb, 0, inb.length);
            }

            try (BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8)))
            {
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null)
                {
                    response.append(responseLine);
                }
                System.out.println("> passed: " + response.toString());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void launchBrowser(String id) {
        if (id.isEmpty()) {
            System.out.println("> BUMP!! NO AGENT ID PROVIDED!!");
            return;
        }

        String _os = System.getProperty("os.name").toLowerCase();
        String url = "http://localhost:" + PORT + "/login/" + id;

        System.out.println("> host os: " + _os);
        System.out.println("> routing: " + url);

        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI(url));
            } else {
                Runtime runtime = Runtime.getRuntime();
                if (_os.contains("mac")) {
                    runtime.exec("open " + url);
                } else if (_os.contains("nix") || _os.contains("nux")) {
                    runtime.exec("xdg-open " + url);
                } else {
                    System.out.println("> BUMP!! UNHANDLED OS!!");
                }
            }
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }
    }
}
