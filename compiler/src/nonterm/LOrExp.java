package nonterm;

import compiler.ParseState;
import compiler.Token;

public class LOrExp {
    private final LOrExp exp1;
    // op must be '||'
    private final LAndExp exp2;

    private LOrExp(LOrExp exp1, LAndExp exp2) {
        this.exp1 = exp1;
        this.exp2 = exp2;
    }

    private LOrExp(LAndExp exp) {
        this.exp1 = null;
        this.exp2 = exp;
    }

    public static LOrExp parse(ParseState state) {
        LOrExp res = new LOrExp(LAndExp.parse(state));
        while (state.getCurToken().getType() == Token.Type.OR) {
            System.out.println(TYPESTR);
            System.out.println(state.getCurToken());
            state.nextToken();
            LAndExp anoExp = LAndExp.parse(state);
            res = new LOrExp(res, anoExp);
        }
        System.out.println(TYPESTR);
        return res;
    }

    public static final String TYPESTR = "<LOrExp>";
}
