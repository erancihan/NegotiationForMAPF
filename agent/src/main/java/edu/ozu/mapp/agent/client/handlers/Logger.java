package edu.ozu.mapp.agent.client.handlers;

public class Logger {
    private static org.slf4j.Logger logger;

    Logger() {}

    public static Logger getLogger(Class<?> clazz) {
        logger = org.slf4j.LoggerFactory.getLogger(clazz);

        return new Logger();
    }

    public void info(String s) {
        logger.info(s);
    }

    public void info(String AgentID, String str) {
        logger.info(str);
    }
}
