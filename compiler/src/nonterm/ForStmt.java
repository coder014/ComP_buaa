package nonterm;

import compiler.ParseState;

public class ForStmt {
    private final LVal lVal;
    private final Exp exp;

    private ForStmt(LVal lVal, Exp exp) {
        this.lVal = lVal;
        this.exp = exp;
    }

    public static ForStmt parse(ParseState state) {
        LVal t = LVal.parse(state);
                state.nextToken();
        final var res = new ForStmt(t, Exp.parse(state));
                return res;
    }

    public static final String TYPESTR = "<ForStmt>";
}
