package nonterm;

import compiler.ParseState;
import compiler.Token;

public class EqExp {
    private final EqExp exp1;
    private final Token op2;
    private final RelExp exp3;

    private EqExp(EqExp exp1, Token op2, RelExp exp3) {
        this.exp1 = exp1;
        this.op2 = op2;
        this.exp3 = exp3;
    }

    private EqExp(RelExp exp) {
        this.exp1 = null;
        this.op2 = null;
        this.exp3 = exp;
    }

    public static EqExp parse(ParseState state) {
        EqExp res = new EqExp(RelExp.parse(state));
        while (state.getCurToken().getType() == Token.Type.EQL
                || state.getCurToken().getType() == Token.Type.NEQ) {
            System.out.println(TYPESTR);
            Token op = state.getCurToken();
            System.out.println(op);
            state.nextToken();
            RelExp anoExp = RelExp.parse(state);
            res = new EqExp(res, op, anoExp);
        }
        System.out.println(TYPESTR);
        return res;
    }

    public static final String TYPESTR = "<EqExp>";
}
