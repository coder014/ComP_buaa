package nonterm;

import compiler.ParseState;
import compiler.Token;

public class RelExp {
    private final RelExp exp1;
    private final Token op2;
    private final AddExp exp3;

    private RelExp(RelExp exp1, Token op2, AddExp exp3) {
        this.exp1 = exp1;
        this.op2 = op2;
        this.exp3 = exp3;
    }

    private RelExp(AddExp exp) {
        this.exp1 = null;
        this.op2 = null;
        this.exp3 = exp;
    }

    public static RelExp parse(ParseState state) {
        RelExp res = new RelExp(AddExp.parse(state));
        while (state.getCurToken().getType() == Token.Type.LSS
                || state.getCurToken().getType() == Token.Type.GRE
                || state.getCurToken().getType() == Token.Type.LEQ
                || state.getCurToken().getType() == Token.Type.GEQ) {
            System.out.println(TYPESTR);
            Token op = state.getCurToken();
            System.out.println(op);
            state.nextToken();
            AddExp anoExp = AddExp.parse(state);
            res = new RelExp(res, op, anoExp);
        }
        System.out.println(TYPESTR);
        return res;
    }

    public static final String TYPESTR = "<RelExp>";
}
