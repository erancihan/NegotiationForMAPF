package edu.ozu.mapp.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;

public class Glob {
    public ArrayList<String> glob(java.nio.file.Path folder, String glob) throws IOException
    {
        ArrayList<String> matches = new ArrayList<>();
//        String folder_path = folder.endsWith("\\") ? String.valueOf(folder) : folder + java.io.File.separator;
        String pattern = glob.replace("*", ".*?");

        Files
            .find(folder, Integer.MAX_VALUE, (filePath, fileAttr) -> fileAttr.isRegularFile())
            .forEach(file -> {
                if (file.toFile().getName().matches(pattern))
                {
                    matches.add(String.valueOf(file));
                }
            });

//        System.out.println(matches.size());

        return matches;
    }
}
