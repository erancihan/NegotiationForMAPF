package edu.ozu.mapp.agent.client.helpers;

import edu.ozu.mapp.agent.Agent;
import edu.ozu.mapp.utils.Action;
import edu.ozu.mapp.utils.Globals;
import org.springframework.util.Assert;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

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
        return new FileLogger(false).setWorldID(WorldID);
    }

    public FileLogger setAgentID(String agentID) {
        AgentID = agentID;

        return this;
    }

    public FileLogger setWorldID(String worldID) {
        WorldID = worldID;

        return this;
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

    public void logAgentPreNego(String SessID, Agent agent) {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date());

        try {
            FileWriter writer = new FileWriter(getFile(String.valueOf(SHEETS.AGENT.NEGOTIATIONS)), true);
            writer
                    .append(timestamp).append(";")      // timestamp
                    .append(String.format(
                            "{\"name\":\"%s\", \"session_id\":\"%s\", \"path\":\"%s\", \"token\":\"%s\", \"conflict_location\":\"%s\"}",
                            "PRE",
                            SessID,                     // negotiation session id
                            agent.path.toString(),      // agents path before negotiation session
                            agent.GetCurrentTokenC(),   // print token count
                            agent.GetConflictLocation() // conflict location
                    ))
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

    public void LogAgentNegotiationState(String prev_bidding_agent, Agent agent)
    {
        LogAgentNegotiationState(prev_bidding_agent, agent, false);
    }

    public void LogAgentNegotiationState(String prev_bidding_agent, Agent agent, boolean IsAccept)
    {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date());
        String name = IsAccept ? "ACCEPT" : "OFFER";

        try {
            FileWriter writer = new FileWriter(getFile(String.valueOf(SHEETS.AGENT.NEGOTIATIONS)), true);
            writer
                    .append(timestamp).append(";")
                    .append(String.format(
                            "{\"name\":\"%s\", \"turn\":\"%s\", \"contract\":\"%s\"}",
                            name,                                       // ACCEPT or OFFER
                            prev_bidding_agent,                         // whose turn it is
                            Negotiation.getContract(agent).toString()   // contract
                    ))
                    .append(System.lineSeparator())
            ;
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void logAgentPostNego(String SessID, Agent agent) {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date());

        try {
            FileWriter writer = new FileWriter(getFile(String.valueOf(SHEETS.AGENT.NEGOTIATIONS)), true);
            writer
                    .append(timestamp).append(";")      // timestamp
                    .append(String.format(
                            "{\"name\":\"%s\", \"session_id\":\"%s\", \"path\":\"%s\", \"token\":\"%s\", \"is_win\":\"%s\"}",
                            "POST",
                            SessID,                     // negotiation session id
                            agent.path.toString(),      // agents path after negotiation session
                            agent.GetCurrentTokenC(),   // print token count
                            agent.negotiation_result    // print win or lose
                    ))
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
                    .append(timestamp).append(";")              // timestamp
                    .append(String.format(
                            "{\"name\":\"%s\",\"agent_id\":\"%s\",\"agent_name\":\"%s\",\"start\":\"%s\",\"dest\":\"%s\",\"path\":\"%s\",\"path_len\":\"%s\"}",
                            "AGENT_JOIN",
                            agent.AGENT_ID,                     // agent identifier
                            agent.AGENT_NAME,                   // name
                            agent.START.key,                    // source
                            agent.DEST.key,                     // destination
                            agent.path.toString(),              // initial planned path
                            agent.path.size()                   // initial planned path length
                    ))
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
                    .append(String.format(
                            "{\"name\":\"%s\",\"agent_id\":\"%s\",\"path\":\"%s\",\"path_len\":\"%s\",\"negoC\":\"%s\",\"winC\":\"%s\",\"loseC\":\"%s\"}",
                            "LEAVE",                    // agent's interaction with the world
                            agent.AGENT_ID,             // agent identifier
                            agent.path.toString(),      // @LEAVE this is the path taken
                            agent.path.size(),          // @LEAVE this is the taken path length
                            (agent.winC + agent.loseC), // sum w&l
                            agent.winC,                 // sum win
                            agent.loseC                 // sum lose
                    ))
                    .append(System.lineSeparator());
            writer.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void LogAgentInfo(Agent agent) {
        LogAgentInfo(agent, "");
    }

    public void LogAgentInfo(Agent agent, String step) {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date());

        try {
            FileWriter writer = new FileWriter(getFile("INFO"), true);
            writer
                    .append(timestamp).append(";")                  // timestamp
                    .append(String.format(
                            "{\"token_count\":\"%s\",\"time\":\"%s\",\"agent_pos\":\"%s\",,\"step\":\"%s\"}",
                            agent.GetCurrentTokenC(),   // current token count
                            agent.time,                 // agent's internal time step counter
                            agent.POS.key,              // current pos of agent
                            step                        // step name
                    ))
                    .append(System.lineSeparator());
            writer.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void logWorldCreate(HashMap<String, Object> payload) {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date());

        try {
            FileWriter writer = new FileWriter(getWorldFile(), true);
            writer
                    .append(timestamp).append(";")
                    .append(String.format(
                            "{\"name\":\"%s\", \"world_id\":\"%s\", \"dimensions\":\"%s\"}",
                            "CREATE",
                            payload.get("world_id"),
                            payload.get("dimensions")
                    ))
                    .append(System.lineSeparator());
            writer.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void LogWorldJoin(Map<String, String> data) {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date());

        try {
            FileWriter writer = new FileWriter(getWorldFile(), true);
            writer
                    .append(timestamp).append(";")
                    .append(String.format(
                            "{\"name\":\"%s\", \"player_count\":\"%s\", \"time_tick\":\"%s\"}",
                            "JOIN",
                            data.get("player_count"),
                            data.get("time_tick")
                    ))
                    .append(System.lineSeparator());
            writer.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void LogWorldDone(Map<String, String> data, double sim_time_diff) {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date());

        try {
            FileWriter writer = new FileWriter(getWorldFile(), true);
            writer
                    .append(timestamp).append(";")
                    .append(String.format(
                            "{\"name\":\"%s\", \"duration\":\"%s\"}",
                            "DONE",
                            sim_time_diff
                    ))
                    .append(System.lineSeparator());
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void LogWorldStateBroadcast(Map<String, String> data, Timestamp t)
    {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(t);

        try {
            FileWriter writer = new FileWriter(getWorldFile(), true);
            writer
                    .append(timestamp).append(";")
                    .append(String.format(
                            "{\"name\":\"%s\"}",
                            Globals.WorldState.BROADCAST
                    ))
                    .append(System.lineSeparator());
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void LogWorldStateNegotiate(Map<String, String> data, Timestamp t, String state)
    {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(t);

        try {
            FileWriter writer = new FileWriter(getWorldFile(), true);
            writer
                    .append(timestamp).append(";")
                    .append(String.format(
                            "{\"name\":\"%s\", \"state\":\"%s\"}",
                            Globals.WorldState.NEGOTIATE,
                            state
                    ))
                    .append(System.lineSeparator());
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void LogWorldStateMove(Map<String, String> data, Timestamp t)
    {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(t);

        try {
            FileWriter writer = new FileWriter(getWorldFile(), true);
            writer
                    .append(timestamp).append(";")
                    .append(String.format(
                            "{\"name\":\"%s\"}",
                            Globals.WorldState.MOVE
                    ))
                    .append(System.lineSeparator());
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
