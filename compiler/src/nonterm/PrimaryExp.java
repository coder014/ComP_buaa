package nonterm;

import compiler.ParseState;
import compiler.Token;

public class PrimaryExp {
    private final Exp exp;
    private final LVal lVal;
    private final Number number;

    private PrimaryExp(Exp exp) {
        this.exp = exp;
        this.lVal = null;
        this.number = null;
    }
    private PrimaryExp(LVal lVal) {
        this.exp = null;
        this.lVal = lVal;
        this.number = null;
    }
    private PrimaryExp(Number number) {
        this.exp = null;
        this.lVal = null;
        this.number = number;
    }

    public static PrimaryExp parse(ParseState state) {
        final PrimaryExp res;
        if (state.getCurToken().getType() == Token.Type.LPARENT) {
                        state.nextToken();
            res = new PrimaryExp(Exp.parse(state));
                        state.nextToken();
        } else if (state.getCurToken().getType() == Token.Type.IDENFR) {
            res = new PrimaryExp(LVal.parse(state));
        } else {
            res = new PrimaryExp(Number.parse(state));
        }
        return res;
    }

    public static final String TYPESTR = "<PrimaryExp>";
}
