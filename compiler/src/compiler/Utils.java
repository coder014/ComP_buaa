package compiler;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.PushbackInputStream;

public class Utils {
    private static LogLevel logLevel = LogLevel.INFO;

    public static PushbackInputStream getFileAsStream(String filename) {
        try {
            var fis = new FileInputStream(filename);
            return new PushbackInputStream(fis, 16);
        } catch (FileNotFoundException e) {
            logErrorf("File `%s` not found\n", filename);
        }
        return null;
    }

    public static LogLevel getLogLevel() {
        return logLevel;
    }

    public static void setLogLevel(LogLevel logLevel) {
        Utils.logLevel = logLevel;
    }

    private static void logf(LogLevel level, String fmt, Object... args) {
        if (level.ordinal() >= logLevel.ordinal()) {
            System.out.printf("[" + level + "] " + fmt, args);
        }
    }

    public static void logDebugf(String fmt, Object... args) {
        logf(LogLevel.DEBUG, fmt, args);
    }

    public static void logInfof(String fmt, Object... args) {
        logf(LogLevel.INFO, fmt, args);
    }

    public static void logWarnf(String fmt, Object... args) {
        logf(LogLevel.WARN, fmt, args);
    }

    public static void logErrorf(String fmt, Object... args) {
        logf(LogLevel.ERROR, fmt, args);
        System.exit(-1);
    }

    public enum LogLevel {
        DEBUG, INFO, WARN, ERROR
    }
}
