package edu.ozu.mapp.agent.client.handlers;

import edu.ozu.mapp.agent.Agent;
import edu.ozu.mapp.utils.Action;
import org.springframework.util.Assert;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class FileLogger {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(FileLogger.class);

    private File file;
    private String file_path;
    private String WorldID;
    private String AgentID;
    private boolean IsAgent;

    public FileLogger(boolean IsAgent) {
        this.IsAgent = IsAgent;
    }

    public FileLogger(String AgentID, boolean IsAgent) {
        this(IsAgent);

        this.AgentID = AgentID;
    }

    private static class SHEETS {
        enum AGENT {
            COORD("COORD", new String[]{"timestamp", "x-y"}),
            NEGOTIATIONS("NEGOTIATIONS", new String[]{});

            public final String[] COLS;
            private final String name;

            AGENT(String name, String[] cols) {
                this.name = name;
                COLS = cols;
            }

            @Override
            public String toString() {
                return name;
            }
        }

        static class WORLD {

        }
    }

    public static FileLogger CreateAgentLogger(String AgentID)
    {
        return new FileLogger(AgentID, true);
    }

    public static FileLogger CreateWorldLogger(String WorldID)
    {
        return new FileLogger(false);
    }

    public void setAgentID(String agentID) {
        AgentID = agentID;
    }

    public void setWorldID(String worldID) {
        WorldID = worldID;
    }

    private File getFile(String WorldID, String AgentID) throws IOException {
        if (file == null) {
//            logger.debug(String.format("creating file logs/%s-%s.log", AgentID, WorldID));

            file = new File(String.format("logs/%s-%s.log", AgentID, WorldID));
            file.getParentFile().mkdirs();
            if (!file.exists()) {
                file.createNewFile();
            }
        }

        return file;
    }

    private File getWorldFile() throws IOException {
        return getFile("", false);
    }

    private File getFile(String Type) throws IOException {
        return getFile(Type, IsAgent);
    }

    private File getFile(String Type, boolean IsAgentLog) throws IOException {
        Assert.isTrue(!WorldID.isEmpty(), "World ID cannot be empty");

        String file_path;
        if (IsAgentLog) {
            Assert.isTrue(!AgentID.isEmpty(), "Agent ID cannot be empty");
            file_path = String.format("logs/WORLD-%s/AGENT-%s-%s.log", WorldID, Type, AgentID);
        } else {
            file_path = String.format("logs/WORLD-%s/WORLD.log", WorldID);
        }
        logger.debug(String.format("opening log file %s", file_path));

        // open file if exists
        File file = new File(file_path);
        file.getParentFile().mkdirs();
        if (!file.exists()) {
            file.createNewFile();
        }

        return file;
    }

    public void logA(String str) {
        Assert.isTrue(!AgentID.isEmpty(), "Agent ID cannot be empty");
        Assert.isTrue(!WorldID.isEmpty(), "World ID cannot be empty");

        new Thread(() -> {
            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date());

            try {
                FileWriter writer = new FileWriter(getFile(WorldID, AgentID), true);
                writer.append(timestamp).append(" - ").append(str).append(System.lineSeparator());
                writer.close();
            } catch (IOException e) {
//                logger.error(e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

    public void logAgentPOS(Agent agent) {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date());

        try {
            FileWriter writer = new FileWriter(getFile(String.valueOf(SHEETS.AGENT.COORD)), true);
            writer
                    .append(timestamp).append(";")                  // timestamp
                    .append(String.valueOf(agent.time)).append(";") // agent's internal time step counter
                    .append(agent.POS.key).append(";")              // agent's location
                    .append(System.lineSeparator());
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void logAgentPreNego(String sessID, Agent agent) {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date());

        try {
            FileWriter writer = new FileWriter(getFile(String.valueOf(SHEETS.AGENT.NEGOTIATIONS)), true);
            writer
                    .append(timestamp).append(";")                      // timestamp
                    .append("PRE" ).append(";")                         // state
                    .append(sessID).append(";")                         // negotiation session id
                    .append(agent.path.toString()).append(";")          // agents path before negotiation session
                    .append(agent.GetCurrentTokenC()).append(";")       // print token count
                    .append(agent.GetConflictLocation()).append(";")    // conflict location
                    .append(System.lineSeparator());
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void logAgentActNego(Action action, Agent agent) {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date());

        try {
            FileWriter writer = new FileWriter(getFile(String.valueOf(SHEETS.AGENT.NEGOTIATIONS)), true);
            writer
                    .append(timestamp).append(";")
                    .append("ACTION" ).append(";")
                    .append(action.toString()).append(";")
                    .append(System.lineSeparator())
            ;
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void logAgentPostNego(String sessID, Agent agent) {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date());

        try {
            FileWriter writer = new FileWriter(getFile(String.valueOf(SHEETS.AGENT.NEGOTIATIONS)), true);
            writer
                    .append(timestamp).append(";")                      // timestamp
                    .append("POST").append(";")                         // state
                    .append(sessID).append(";")                         // negotiation session id
                    .append(agent.path.toString()).append(";")          // agents path after negotiation session
                    .append(agent.GetCurrentTokenC()).append(";")       // print token count
                    .append(System.lineSeparator());
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void logAgentWorldJoin(Agent agent) {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date());

        try {
            /* In post process, this data will be joined by LEAVE */
            FileWriter writer = new FileWriter(getWorldFile(), true);
            writer
                    .append(timestamp).append(";")                          // timestamp
                    .append("JOIN").append(";")                             // agent's interaction with the world
                    .append(agent.AGENT_ID).append(";")                     // agent identifier
                    .append(agent.AGENT_NAME).append(";")                   // name
                    .append("START:").append(agent.START.key).append(";")   // source
                    .append("DEST:").append(agent.DEST.key).append(";")     // destination
                    .append(agent.path.toString()).append(";")              // initial planned path
                    .append(String.valueOf(agent.path.size())).append(";")  // initial planned path length
                    // TODO initial token count
                    .append(System.lineSeparator());
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void logAgentWorldLeave(Agent agent) {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date());

        try {
            FileWriter writer = new FileWriter(getWorldFile(), true);
            writer
                    .append(timestamp).append(";")                          // timestamp
                    .append("LEAVE").append(";")                            // agent's interaction with the world
                    .append(agent.AGENT_ID).append(";")                     // agent identifier
                    .append(agent.path.toString()).append(";")              // @LEAVE this is the path taken
                    .append(String.valueOf(agent.path.size())).append(";")  // @LEAVE this is the taken path length
                    // TODO nego count
                    .append(String.valueOf(agent.winC)).append(";")         // sum win
                    .append(String.valueOf(agent.loseC)).append(";")        // sum lose
                    .append(System.lineSeparator());
            writer.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void logAgentNotify() {

    }

    public void logWorldCreate() {
        // TODO log world configuration
    }
}
