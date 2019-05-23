package edu.ozu.drone;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.awt.*;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;

@SpringBootApplication
public class Runner {
    private static String PORT = "8080";

    public static void main(String[] args) {
        try {
            System.setProperty("server.port", PORT);
            System.setProperty("spring.devtools.restart.enabled", "false");
            SpringApplication.run(Runner.class, args);

            init();
        } catch (ClassNotFoundException e) {
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
                    runtime.exec("xdg-open" + url);
                } else {
                    System.out.println("> BUMP!! UNHANDLED OS!!");
                }
            }
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }
    }

    private static void init() throws ClassNotFoundException {
        Set<Class> stuff = getRunTargets();

        stuff.forEach(Runner::initClass);
    }

    private static void initClass(Class v) {
        try {
            AgentClient x = (AgentClient) Class.forName(v.getName()).getConstructor().newInstance();

            if (!x.agentID().isEmpty())
            {
                launchBrowser(x.agentID());
            }

            Thread thread = new Thread(x);
            thread.start();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException | ClassNotFoundException e) {
            e.printStackTrace();
        }
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
}
