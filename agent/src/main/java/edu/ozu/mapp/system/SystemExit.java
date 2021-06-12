package edu.ozu.mapp.system;

import java.util.ArrayList;
import java.util.function.Consumer;

public class SystemExit
{
    public static ArrayList<Consumer<Integer>> ExitHooks = new ArrayList<>();

    public static boolean SHUTDOWN_ON_EXIT = true;
    public static int EXIT_CODE = 0;

    enum Status {
        TIMEOUT(501), FATAL(500);

        int value;
        Status(int status) {
            this.value = status;
        }
    }

    public static void exit(Status status) {
        exit(status.value);
    }

    public static void exit(int status)
    {
        for (Consumer<Integer> hook : ExitHooks)
        {
            hook.accept(status);
        }

        if (SHUTDOWN_ON_EXIT)
        {
            System.exit(status);
        }
    }

    public static void hook(Consumer<Integer> runnable, int index)
    {
        ExitHooks.add(index, runnable);
    }

    public static void hook(Consumer<Integer> runnable)
    {
        ExitHooks.add(runnable);
    }
}
