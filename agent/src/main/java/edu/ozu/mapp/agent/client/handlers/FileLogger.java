package edu.ozu.mapp.agent.client.handlers;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class FileLogger {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(FileLogger.class);
    private static FileLogger fl = null;

    private File file;

    private FileLogger() {

    }

    public static FileLogger getInstance() {
        if (fl == null) {
            fl = new FileLogger();
        }

        return fl;
    }

    private File getFile(String WorldID, String AgentID) throws IOException {
        if (file == null) {
            logger.debug(String.format("creating file logs/%s-%s.log", AgentID, WorldID));

            file = new File(String.format("logs/%s-%s.log", AgentID, WorldID));
            file.getParentFile().mkdirs();
            if (!file.exists()) {
                file.createNewFile();
            }
        }

        return file;
    }

    public void log(String WorldID, String AgentID, String str) {
        new Thread(() -> {
            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date());

            try {
                FileWriter writer = new FileWriter(getFile(WorldID, AgentID), true);
                writer.append(timestamp).append(" - ").append(str).append(System.lineSeparator());
                writer.close();
            } catch (IOException e) {
                logger.error(e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }
}
