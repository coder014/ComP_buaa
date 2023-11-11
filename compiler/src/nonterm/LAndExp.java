package nonterm;

import compiler.ParseState;
import compiler.Token;

public class LAndExp {
    private final LAndExp exp1;
    // op must be '&&'
    private final EqExp exp2;

    private LAndExp(LAndExp exp1, EqExp exp2) {
        this.exp1 = exp1;
        this.exp2 = exp2;
    }

    private LAndExp(EqExp exp) {
        this.exp1 = null;
        this.exp2 = exp;
    }

    public static LAndExp parse(ParseState state) {
        LAndExp res = new LAndExp(EqExp.parse(state));
        while (state.getCurToken().getType() == Token.Type.AND) {
                                    state.nextToken();
            EqExp anoExp = EqExp.parse(state);
            res = new LAndExp(res, anoExp);
        }
                return res;
    }

    public static final String TYPESTR = "<LAndExp>";
}
