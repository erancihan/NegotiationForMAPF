package edu.ozu.drone;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Set;

@SpringBootApplication
public class Runner {
    public static void main(String[] args) {
        try {
            run();
            SpringApplication.run(Runner.class, args);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private static void run() throws ClassNotFoundException {
        Set<Class> stuff = getRunTargets();

        stuff.forEach(Runner::initClass);
    }

    private static void initClass(Class v) {
        try {
            AgentClient x = (AgentClient) Class.forName(v.getName()).getConstructor().newInstance();

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
