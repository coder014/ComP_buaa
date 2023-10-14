package nonterm;

import compiler.ParseState;
import compiler.Token;

public class UnaryExp {
    private final PrimaryExp pExp;
    private final UnaryOp uOp;
    private final UnaryExp uExp;
    private final Token ident;
    private final FuncRParams params;

    private UnaryExp(PrimaryExp pExp) {
        this.pExp = pExp;
        this.uOp = null;
        this.uExp = null;
        this.ident = null;
        this.params = null;
    }
    private UnaryExp(UnaryOp op, UnaryExp exp) {
        this.pExp = null;
        this.uOp = op;
        this.uExp = exp;
        this.ident = null;
        this.params = null;
    }
    private UnaryExp(Token ident, FuncRParams params) {
        this.pExp = null;
        this.uOp = null;
        this.uExp = null;
        this.ident = ident;
        this.params = params;
    }

    public static UnaryExp parse(ParseState state) {
        final UnaryExp res;
        if (state.getCurToken().getType() == Token.Type.PLUS
                || state.getCurToken().getType() == Token.Type.MINU
                || state.getCurToken().getType() == Token.Type.NOT) {
            final var op = UnaryOp.parse(state);
            res = new UnaryExp(op, parse(state));
        } else {
            final Token t1, t2;
            t1 = state.getCurToken(); state.nextToken();
            t2 = state.getCurToken(); state.ungetToken();
            if (t1.getType() == Token.Type.IDENFR
                    && t2.getType() == Token.Type.LPARENT) {
                res = parseFuncCall(state);
            } else {
                res = new UnaryExp(PrimaryExp.parse(state));
            }
        }
        System.out.println(TYPESTR);
        return res;
    }

    private static UnaryExp parseFuncCall(ParseState state) {
        final UnaryExp res;
        final var ident = state.getCurToken();
        System.out.println(ident);
        state.nextToken();
        System.out.println(state.getCurToken());
        state.nextToken();
        if (state.getCurToken().getType() != Token.Type.RPARENT) {
            res = new UnaryExp(ident, FuncRParams.parse(state));
        } else {
            res = new UnaryExp(ident, null);
        }
        System.out.println(state.getCurToken());
        state.nextToken();
        return res;
    }

    public static final String TYPESTR = "<UnaryExp>";
}
