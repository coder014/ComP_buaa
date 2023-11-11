package nonterm;

import compiler.ParseState;
import compiler.Token;

public class MulExp {
    private final MulExp exp1;
    private final Token op2;
    private final UnaryExp exp3;

    private MulExp(MulExp exp1, Token op2, UnaryExp exp3) {
        this.exp1 = exp1;
        this.op2 = op2;
        this.exp3 = exp3;
    }
    private MulExp(UnaryExp exp) {
        this.exp1 = null;
        this.op2 = null;
        this.exp3 = exp;
    }

    public static MulExp parse(ParseState state) {
        MulExp res = new MulExp(UnaryExp.parse(state));
        while (state.getCurToken().getType() == Token.Type.MULT
                || state.getCurToken().getType() == Token.Type.DIV
                || state.getCurToken().getType() == Token.Type.MOD) {
                        Token op = state.getCurToken();
                        state.nextToken();
            UnaryExp anoExp = UnaryExp.parse(state);
            res = new MulExp(res, op, anoExp);
        }
        return res;
    }

    public static final String TYPESTR = "<MulExp>";
}
