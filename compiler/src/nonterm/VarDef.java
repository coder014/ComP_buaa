package nonterm;

import compiler.ParseState;
import compiler.Token;
import symbol.CompError;

import java.util.ArrayList;
import java.util.List;

public class VarDef {
    private final Token ident;
    private final List<ConstExp> exps;
    private final InitVal val;

    private VarDef(Token ident, List<ConstExp> exps, InitVal val) {
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
    public InitVal getVal() {
        return val;
    }

    public static VarDef parse(ParseState state) {
        final VarDef res;
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
                CompError.appendError(state.getCurToken().getLineNum(), 'k', "Missing `]` as VarDef arr[]");
                state.nextToken();
            }
        }
        if (state.getCurToken().getType() == Token.Type.ASSIGN) {
            state.nextToken();
            res = new VarDef(token, list, InitVal.parse(state));
        } else {
            res = new VarDef(token, list, null);
        }
        return res;
    }

    public static final String TYPESTR = "<VarDef>";
}
