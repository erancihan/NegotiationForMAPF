package edu.ozu.mapp.utils;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class Save {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Save.class);

    public static void stringToFile(String data, String file)
    {
        file = "logs/" + file;
        try
        {
            logger.info("writing to file " + file);
            FileWriter writer = new FileWriter(file);

            writer.write(data);

            writer.close();
            logger.info("done writing to " + file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void listToFile(List<Object[]> items, String file)
    {
        file += "logs/" + file;
        try
        {
            logger.info("writing to file " + file);
            FileWriter writer = new FileWriter(file);
            for (Object[] objects : items)
            {
                writer.write(Arrays.toString(objects) + System.lineSeparator());
            }
            writer.close();
            logger.info("done writing to " + file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
