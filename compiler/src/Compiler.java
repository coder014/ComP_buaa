import compiler.Lexer;
import compiler.ParseState;
import compiler.Utils;
import nonterm.CompUnit;

import java.io.FileNotFoundException;
import java.io.PrintStream;

public class Compiler {
    public static void main(String[] args) {
//      compiler.Utils.setLogLevel(compiler.Utils.LogLevel.DEBUG);
        try {
            System.setOut(new PrintStream("output.txt"));
        } catch (FileNotFoundException ignored) {}
        final var is = Utils.getFileAsStream("testfile.txt");
        final var state = new ParseState(new Lexer(is));
        CompUnit.parse(state);
    }
}