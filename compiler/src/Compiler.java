import compiler.Lexer;
import compiler.ParseState;
import compiler.Utils;
import nonterm.CompUnit;

public class Compiler {
    public static void main(String[] args) {
        compiler.Utils.setLogLevel(compiler.Utils.LogLevel.DEBUG);
        final var is = Utils.getFileAsStream("testfile.txt");
        final var state = new ParseState(new Lexer(is));
        CompUnit.parse(state);
    }
}