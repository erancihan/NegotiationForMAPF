package edu.ozu.mapp.utils;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashSet;

public class Glob {
    public ArrayList<String> glob(java.nio.file.Path folder, String glob) throws IOException
    {
        ArrayList<String> matches = new ArrayList<>();
//        String folder_path = folder.endsWith("\\") ? String.valueOf(folder) : folder + File.separator;
        String pattern = "glob:" + glob;

        PathMatcher matcher = FileSystems.getDefault().getPathMatcher(pattern);
        Files.walkFileTree(folder, new HashSet<>(), 1, new SimpleFileVisitor<java.nio.file.Path>() {
            @Override
            public FileVisitResult visitFile(java.nio.file.Path file, BasicFileAttributes attributes) {
                if (matcher.matches(file.getFileName())) {
                    matches.add(String.valueOf(file));
                }

                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                return FileVisitResult.CONTINUE;
            }
        });
        System.out.println(pattern);

        return matches;
    }
}
