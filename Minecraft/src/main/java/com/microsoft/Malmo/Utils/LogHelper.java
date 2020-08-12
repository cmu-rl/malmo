package com.microsoft.Malmo.Utils;
import java.util.HashSet;

public class LogHelper {

    private static HashSet<Integer> silenced = new HashSet<Integer>();

    public static void debug(String message) {
        System.out.println("[DEBUG] " + message);
    }

    public static void debugOnce(String message) {
        // only show once
        int hashCode = hashStack();
        if (silenced.contains(hashCode)) {
            return;
        }
        silenced.add(hashCode);

        debug(message);
    }

    public static void error(String message) {
        System.err.println("[DEBUG] " + message);
    }

    public static void error(String message, Exception e) {
        if (e != null) {
            message += ": " + e;
        }

        error(message);

        e.printStackTrace(System.err);
    }

    private static int hashStack() {
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        StringBuilder builder = new StringBuilder();
        // skip calls: getStackTrace, hashStack
        for (int i = 2; i < stackTraceElements.length; i++) {
            StackTraceElement ste = stackTraceElements[i];
            builder.append(ste.getClassName());
            builder.append(".");
            builder.append(ste.getMethodName());
            builder.append("|");
        }
        return builder.toString().hashCode();
    }

}
