package nonterm;

import compiler.ParseState;
import compiler.Token;
import symbol.CompError;

import java.util.ArrayList;
import java.util.List;

public class ConstDef {
    private final Token ident;
    private final List<ConstExp> exps;
    private final ConstInitVal val;

    private ConstDef(Token ident, List<ConstExp> exps, ConstInitVal val) {
        this.ident = ident;
        this.exps = exps;
        this.val = val;
    }

    public Token getIdent() {
        return ident;
    }
    public List<ConstExp> getExps() {
        return exps;
    }
    public ConstInitVal getVal() {
        return val;
    }

    public static ConstDef parse(ParseState state) {
        final Token token = state.getCurToken();
        final List<ConstExp> list = new ArrayList<>();
        state.nextToken();
        while (state.getCurToken().getType() == Token.Type.LBRACK) {
            state.nextToken();
            list.add(ConstExp.parse(state));
            if (state.getCurToken().getType() == Token.Type.RBRACK) {
                state.nextToken();
            } else {
                state.ungetToken();
                CompError.appendError(state.getCurToken().getLineNum(), 'k', "Missing `]` as ConstDef arr[]");
                state.nextToken();
            }
        }
        state.nextToken();
        return new ConstDef(token, list, ConstInitVal.parse(state));
    }

    public static final String TYPESTR = "<ConstDef>";
}
