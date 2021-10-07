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
        String[] confs = new String[]{"16x16_20", "16x16_60", "8x8_15", "8x8_20", "16x16_40", "16x16_80"};

        for (String conf : confs)
        {
            try
            {
                ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
                root.setLevel(Level.INFO);

                System.out.println(FileSystemView.getFileSystemView().getDefaultDirectory().getPath());
                System.out.println(System.getProperty("user.dir"));

                Path path = Paths.get(
                        FileSystemView.getFileSystemView().getDefaultDirectory().getPath(), "MAPP",    // %HOME%/Documents/MAPP
                        "scenarios",
                        "PathAwareAgent",
                        conf
                );
                ArrayList<String> scenarios = new Glob().glob(path, "world-scenario-*.json");

// SET FIELD OF VIEW
                Globals.FIELD_OF_VIEW_SIZE = 5;     // d = 2 from current location to sides
                Globals.BROADCAST_SIZE = 5;     // 2d + 1
//            Globals.FIELD_OF_VIEW_SIZE  = 7;     // d = 3 from current location to sides
//            Globals.BROADCAST_SIZE      = 7;     // 2d + 1
//            Globals.FIELD_OF_VIEW_SIZE  = 9;     // d = 4 from current location to sides
//            Globals.BROADCAST_SIZE      = 9;     // 2d + 1

// SET MOVE ACTION SPACE SIZE
                Globals.MOVE_ACTION_SPACE_SIZE = 4; //   no wait action
                // Globals.MOVE_ACTION_SPACE_SIZE = 5; // with wait action

// SET LEAVE ACTION BEHAVIOUR
//                Globals.LEAVE_ACTION_BEHAVIOUR = LeaveActionHandler.LeaveActionTYPE.OBSTACLE;      // obstacle on exit
                 Globals.LEAVE_ACTION_BEHAVIOUR = LeaveActionHandler.LeaveActionTYPE.PASS_THROUGH; //    leave on exit

// SET BID SEARCH SPACE OVERRIDE
                Globals.BID_SEARCH_STRATEGY_OVERRIDE = BidSpace.SearchStrategy.POP_LAST;
//            Globals.MAX_BID_SPACE_POOL_SIZE = 100;

// SET WORLD TIMEOUT
//            Globals.STALE_NEGOTIATE_STATE_WAIT_COUNTER_LIMIT = 300;
//            Globals.STATE_SIMULATION_PROCESS_COUNTER_LIMIT   = 400;

// TOURNAMENT CONFIG
                TournamentRunner.TOURNAMENT_RUNNER_MAX_NUMBER_OF_TRIES = 2; // retry once
                SystemExit.SHUTDOWN_ON_EXIT = false;

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
