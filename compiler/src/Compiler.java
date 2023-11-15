import compiler.Lexer;
import compiler.ParseState;
import compiler.Utils;
import compiler.Visitor;
import nonterm.CompUnit;
import symbol.CompError;

public class Compiler {
    public static void main(String[] args) {
        compiler.Utils.setLogLevel(Utils.LogLevel.ERROR);
        final var is = Utils.getFileAsStream("testfile.txt");
        final var state = new ParseState(new Lexer(is));
        final var comp = CompUnit.parse(state);
        Visitor.visitCompUnit(comp);
        if (CompError.hasError()) CompError.printErrors();
    }
}