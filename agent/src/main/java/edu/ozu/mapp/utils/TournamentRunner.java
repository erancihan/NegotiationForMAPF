package edu.ozu.mapp.utils;

import edu.ozu.mapp.agent.client.world.ScenarioManager;

import javax.swing.filechooser.FileSystemView;
import java.io.File;
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

//        Path path = Paths.get(FileSystemView.getFileSystemView().getDefaultDirectory().getPath(), "MAPP");
            Path path = Paths.get(System.getProperty("user.dir"), "artifacts", "configs");
            ArrayList<String> scenarios = new Glob().glob(path, "*.json");

            new TournamentRunner().run(scenarios);
        } catch (Exception exception) {
            exception.printStackTrace();
            System.exit(1);
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
