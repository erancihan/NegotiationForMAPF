package edu.ozu.mapp.agent.client.handlers;

import edu.ozu.mapp.agent.Agent;
import org.springframework.util.Assert;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Logger;

public class FileLogger extends Logger {
    private File file;
    private String file_path;
    private String WorldID;
    private String AgentID;
    private boolean IsAgent;

    public static Logger getLogger(Class<?> clazz) {
        return null;
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
        return new FileLogger();
    }

    public static FileLogger CreateWorldLogger(String WorldID)
    {
        return new FileLogger();
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

    private File getFile() throws IOException {
        if (file == null) {
            Assert.isTrue(!WorldID.isEmpty(), "World ID cannot be empty");

            if (IsAgent) {
                Assert.isTrue(!AgentID.isEmpty(), "Agent ID cannot be empty");
                file_path = String.format("logs/WORLD-%s/AGENT-%s.log", WorldID, AgentID);
            } else {
                file_path = String.format("logs/WORLD-%s/WORLD.log", WorldID);
            }
//            logger.debug(String.format("opening log file %s", file_path));

            // open file if exists
            file = new File(file_path);
            file.getParentFile().mkdirs();
            if (!file.exists()) {
                file.createNewFile();
            }

            try (FileOutputStream stream = new FileOutputStream(file)) {
//                workbook.write(stream);
            }

            return file;
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

        Assert.notNull("", "SHEET " + SHEETS.AGENT.COORD + " DOES NOT EXIST");
    }

    public void logW(String str) {
        Assert.isTrue(!WorldID.isEmpty(), "World ID cannot be empty");
    }
}
