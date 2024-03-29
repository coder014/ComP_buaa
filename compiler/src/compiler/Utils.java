package compiler;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.io.PushbackInputStream;

public class Utils {
    private static LogLevel logLevel = LogLevel.INFO;
    private static PrintStream originOutStream;
    private static ByteArrayOutputStream bufferStream;
    private static int counter = -1;

   /* public static void freezeOutput() {
        originOutStream = System.out;
        bufferStream = new ByteArrayOutputStream(4096);
        System.setOut(new PrintStream(bufferStream));
    }
    public static void resumeOutput() {
        System.setOut(originOutStream);
        if (bufferStream != null) {
            final var buf = bufferStream.toByteArray();
            System.out.write(buf, 0, buf.length);
        }
    }
    public static void discardOutput() {
        bufferStream = null;
    }*/

    public static void resetCounter() {
        counter = -1;
    }
    public static int getIncCounter() {
        return ++counter;
    }
    public static int getCounter() {
        return counter;
    }

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

    public static int substrCount(String str, String sub) {
        int count = 0;
        final int sublen = sub.length();
        int index = str.indexOf(sub);
        while (index != -1) {
            count++;
            index = str.indexOf(sub, index + sublen);
        }
        return count;
    }

    public enum LogLevel {
        DEBUG, INFO, WARN, ERROR
    }
}
