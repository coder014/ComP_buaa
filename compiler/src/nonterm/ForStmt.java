package nonterm;

import compiler.ParseState;

public class ForStmt {
    private final LVal lVal;
    private final Exp exp;

    private ForStmt(LVal lVal, Exp exp) {
        this.lVal = lVal;
        this.exp = exp;
    }

    public LVal getLVal() {
        return lVal;
    }
    public Exp getExp() {
        return exp;
    }

    public static ForStmt parse(ParseState state) {
        LVal t = LVal.parse(state);
        state.nextToken();
        return new ForStmt(t, Exp.parse(state));
    }

    public static final String TYPESTR = "<ForStmt>";
}
