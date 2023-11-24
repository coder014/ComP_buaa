package ir;

import java.util.ArrayList;
import java.util.List;

public class Module implements IRPrintable {
    private final List<GlobalVar> globalVars = new ArrayList<>();
    private final List<Function> functions = new ArrayList<>();
    public final static Module INSTANCE = new Module();
    private Module() {}

    public void appendGlobalVar(GlobalVar var) {
        this.globalVars.add(var);
    }
    public void appendFunction(Function func) {
        this.functions.add(func);
    }

    @Override
    public void emitString(StringBuilder sb) {
        sb.append("declare i32 @getint()\ndeclare void @putint(i32)\ndeclare void @putch(i32)\n\n");
        for (final var gv : globalVars) {
            gv.emitString(sb);
            sb.append('\n');
        }
        sb.append('\n');
        for (final var fn : functions) {
            fn.emitString(sb);
            sb.append("\n\n");
        }
    }
}
