package symbol;

import compiler.Utils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class CompError {
    public static final List<CompError> errorList = new ArrayList<>();

    private final int lineNum;
    private final char type;
    private final String description;

    private CompError(int lineNum, char type, String description) {
        this.lineNum = lineNum;
        this.type = type;
        this.description = description;
    }

    public int getLineNum() {
        return lineNum;
    }

    public char getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return String.format("%d %c", lineNum, type);
    }

    public static void appendError(int lineNum, char type, String description) {
        errorList.add(new CompError(lineNum, type, description));
        Utils.logWarnf("Got error $%c$ at line %d, reason: %s\n", type, lineNum, description);
    }

    public static void printErrors() {
        errorList.sort(Comparator.comparingInt(o -> o.lineNum));
        for (var err : errorList) {
            System.out.println(err);
        }
    }
}