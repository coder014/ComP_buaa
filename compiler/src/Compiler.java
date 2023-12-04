import compiler.Lexer;
import compiler.ParseState;
import compiler.Utils;
import compiler.Visitor;
import ir.Module;
import nonterm.CompUnit;
import symbol.CompError;

import java.io.FileNotFoundException;
import java.io.PrintStream;

public class Compiler {
    public static void main(String[] args) {
        compiler.Utils.setLogLevel(Utils.LogLevel.ERROR);
        final var is = Utils.getFileAsStream("testfile.txt");
        final var state = new ParseState(new Lexer(is));
        final var comp = CompUnit.parse(state);
        Visitor.visitCompUnit(comp);
        if (CompError.hasError()) CompError.printErrors();
        else {
            final StringBuilder sb = new StringBuilder();
            Module.INSTANCE.emitString(sb);
            try {
                new PrintStream("llvm_ir.txt").print(sb);
            } catch (FileNotFoundException ignored) {}
//            System.out.print(sb);
        }
    }
}