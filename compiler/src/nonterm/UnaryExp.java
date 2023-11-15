package nonterm;

import compiler.ParseState;
import compiler.Token;
import symbol.CompError;

public class UnaryExp {
    private final PrimaryExp pExp;
    private final UnaryOp uOp;
    private final UnaryExp uExp;
    private final Token ident;
    private final FuncRParams params;

    public PrimaryExp getPExp() {
        return pExp;
    }
    public UnaryOp getUOp() {
        return uOp;
    }
    public UnaryExp getUExp() {
        return uExp;
    }
    public Token getIdent() {
        return ident;
    }
    public FuncRParams getRParams() {
        return params;
    }

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
        return res;
    }

    private static UnaryExp parseFuncCall(ParseState state) {
        final UnaryExp res;
        final var ident = state.getCurToken();
        state.nextToken();
        state.nextToken();
        if (state.getCurToken().getType() == Token.Type.IDENFR
                || state.getCurToken().getType() == Token.Type.PLUS
                || state.getCurToken().getType() == Token.Type.MINU
                || state.getCurToken().getType() == Token.Type.NOT
                || state.getCurToken().getType() == Token.Type.LPARENT
                || state.getCurToken().getType() == Token.Type.INTCON) {
            res = new UnaryExp(ident, FuncRParams.parse(state));
        } else res = new UnaryExp(ident, null);
        if (state.getCurToken().getType() == Token.Type.RPARENT) {
            state.nextToken();
        } else {
            state.ungetToken();
            CompError.appendError(state.getCurToken().getLineNum(), 'j', "Missing `)` as FuncCall()");
            state.nextToken();
        }
        return res;
    }

    public static final String TYPESTR = "<UnaryExp>";
}
