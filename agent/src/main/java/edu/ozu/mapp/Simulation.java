package edu.ozu.mapp;

import ch.qos.logback.classic.Level;
import edu.ozu.mapp.system.LeaveActionHandler;
import edu.ozu.mapp.system.SystemExit;
import edu.ozu.mapp.utils.Glob;
import edu.ozu.mapp.utils.Globals;
import edu.ozu.mapp.utils.TournamentRunner;
import edu.ozu.mapp.utils.bid.BidSpace;

import javax.swing.filechooser.FileSystemView;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public class Simulation {
    @SuppressWarnings("DuplicatedCode")
    public static void main(String[] args)
    {
        System.out.println(System.getProperty("os.name"));
        System.out.println(FileSystemView.getFileSystemView().getDefaultDirectory().getPath());
        Path home = System.getProperty("os.name").toLowerCase().contains("win")
                ? Paths.get(FileSystemView.getFileSystemView().getDefaultDirectory().getPath(), "MAPP") // %HOME%/Documents, MAPP
                : Paths.get(FileSystemView.getFileSystemView().getDefaultDirectory().getPath(), "Documents", "MAPP"); // ~, Documents, MAPP

        String[][] confs = new String[][]{
//                {"PathAwareAgent", "16x16_60"}
//                {"HeightMapAgent", "16x16_40"}
                {"HeightMapAgentMEMOConstraint", "16x16_80", "9", "5", "leave"},
                {"PathAwareAgentMEMOConstraint", "16x16_80", "9", "5", "leave"}
        };

        for (String[] conf : confs)
        {
            try
            {
                ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
                root.setLevel(Level.INFO);

                Path path = Paths.get(String.valueOf(home), "scenarios", conf[0], conf[1]);
                ArrayList<String> scenarios = new Glob().glob(path, "world-scenario-*.json");

// SET FIELD OF VIEW
                Globals.FIELD_OF_VIEW_SIZE  = Integer.parseInt(conf[2]);
                Globals.BROADCAST_SIZE      = Integer.parseInt(conf[2]);

// SET MOVE ACTION SPACE SIZE
                Globals.MOVE_ACTION_SPACE_SIZE = Integer.parseInt(conf[3]);

// SET LEAVE ACTION BEHAVIOUR
                Globals.LEAVE_ACTION_BEHAVIOUR = conf[4].equals("leave")
                        ? LeaveActionHandler.LeaveActionTYPE.PASS_THROUGH
                        : LeaveActionHandler.LeaveActionTYPE.OBSTACLE;

// SET COMMITMENT SIZE OVERRIDE
//                Globals.MAX_COMMITMENT_SIZE = 0;

// SET BID SEARCH SPACE OVERRIDE
                Globals.BID_SEARCH_STRATEGY_OVERRIDE = BidSpace.SearchStrategy.BFS;

// SET WORLD TIMEOUT
                Globals.STALE_NEGOTIATE_STATE_WAIT_COUNTER_LIMIT = 60;
                Globals.STALE_SIMULATION_PROCESS_COUNTER_LIMIT = 120;

// TOURNAMENT CONFIG
                TournamentRunner.TOURNAMENT_RUNNER_MAX_NUMBER_OF_TRIES = 100; // retry
                SystemExit.SHUTDOWN_ON_EXIT = false;
                Globals.LOG_BID_SPACE = true;

                File tournament_run_results = Paths.get(path.toString(), "runs.txt").toFile();
                //noinspection ResultOfMethodCallIgnored
                tournament_run_results.getParentFile().mkdirs();
                if (!tournament_run_results.exists())
                {
                    //noinspection ResultOfMethodCallIgnored
                    tournament_run_results.createNewFile();
                }

                TournamentRunner runner = new TournamentRunner();
                runner.tournament_run_results = tournament_run_results;
                runner.homedir = home;
                runner.run(scenarios);
            }
            catch (Exception exception)
            {
                exception.printStackTrace();
                System.exit(1);
            }
        }
    }
}
