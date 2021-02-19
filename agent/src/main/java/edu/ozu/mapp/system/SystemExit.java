package edu.ozu.mapp.system;

import java.util.function.Consumer;

public class SystemExit
{
    public static Consumer<Integer> ExitHook;
    public static boolean SHUTDOWN_ON_EXIT = true;

    public static void exit(int status)
    {
        if (ExitHook != null) {
            ExitHook.accept(status);
        }

//        if (SHUTDOWN_ON_EXIT) {
            System.exit(status);
//        }
    }
}
