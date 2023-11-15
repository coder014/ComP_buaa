package nonterm;

import compiler.ParseState;
import compiler.Token;

public class AddExp {
    private final AddExp exp1;
    private final Token op2;
    private final MulExp exp3;

    private AddExp(AddExp exp1, Token op2, MulExp exp3) {
        this.exp1 = exp1;
        this.op2 = op2;
        this.exp3 = exp3;
    }
    private AddExp(MulExp exp) {
        this.exp1 = null;
        this.op2 = null;
        this.exp3 = exp;
    }

    public AddExp getLExp() {
        return exp1;
    }
    public Token getOp() {
        return op2;
    }
    public MulExp getRExp() {
        return exp3;
    }

    public static AddExp parse(ParseState state) {
        AddExp res = new AddExp(MulExp.parse(state));
        while (state.getCurToken().getType() == Token.Type.PLUS
                || state.getCurToken().getType() == Token.Type.MINU) {
            Token op = state.getCurToken();
            state.nextToken();
            MulExp anoExp = MulExp.parse(state);
            res = new AddExp(res, op, anoExp);
        }
        return res;
    }

    public static final String TYPESTR = "<AddExp>";
}
