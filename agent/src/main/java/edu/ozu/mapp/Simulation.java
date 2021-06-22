package edu.ozu.mapp;

import ch.qos.logback.classic.Level;
import edu.ozu.mapp.system.LeaveActionHandler;
import edu.ozu.mapp.system.SystemExit;
import edu.ozu.mapp.utils.Glob;
import edu.ozu.mapp.utils.Globals;
import edu.ozu.mapp.utils.TournamentRunner;

import javax.swing.filechooser.FileSystemView;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public class Simulation {
    public static void main(String[] arg) {
        try {
            ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
            root.setLevel(Level.INFO);

            System.out.println(FileSystemView.getFileSystemView().getDefaultDirectory().getPath());
            System.out.println(System.getProperty("user.dir"));

            Path path = Paths.get(System.getProperty("user.dir"), "artifacts", "configs", "16x16_40_Hybrid");
            ArrayList<String> scenarios = new Glob().glob(path, "world-scenario-*.json");

            Globals.FIELD_OF_VIEW_SIZE = 7;
            Globals.LEAVE_ACTION_BEHAVIOUR = LeaveActionHandler.LeaveActionTYPE.OBSTACLE;
            Globals.MOVE_ACTION_SPACE_SIZE = 5;
            SystemExit.SHUTDOWN_ON_EXIT = false;

            File tournament_run_results = Paths.get(path.toString(), "runs.txt").toFile();
            //noinspection ResultOfMethodCallIgnored
            tournament_run_results.getParentFile().mkdirs();
            if (!tournament_run_results.exists()) {
                //noinspection ResultOfMethodCallIgnored
                tournament_run_results.createNewFile();
            }

            TournamentRunner runner = new TournamentRunner();
            runner.tournament_run_results = tournament_run_results;
            runner.run(scenarios);
        } catch (Exception exception) {
            exception.printStackTrace();
            System.exit(1);
        }
    }
}
