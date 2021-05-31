package edu.ozu.mapp.system;

import java.util.function.Consumer;

public class SystemExit
{
    public static Consumer<Integer> ExitHook = null;

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
        if (ExitHook == null) {
            // default behaviour
            System.exit(status);
        }

        ExitHook.accept(status);
    }
}
