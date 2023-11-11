package nonterm;

import compiler.ParseState;
import compiler.Token;
import symbol.CompError;

import java.util.ArrayList;
import java.util.List;

public class FuncFParam {
    private final BType type;
    private final Token ident;
    private final List<ConstExp> exps = new ArrayList<>();

    private FuncFParam(BType type, Token ident) {
        this.type = type;
        this.ident = ident;
    }

    private void appendExp(ConstExp exp) {
        exps.add(exp);
    }

    public static FuncFParam parse(ParseState state) {
        final var bType = BType.parse(state);
        final FuncFParam res = new FuncFParam(bType, state.getCurToken());
        state.nextToken();
        if (state.getCurToken().getType() == Token.Type.LBRACK) {
            state.nextToken();
            res.appendExp(null);
            if (state.getCurToken().getType() == Token.Type.RBRACK) {
                state.nextToken();
            } else {
                state.ungetToken();
                CompError.appendError(state.getCurToken().getLineNum(), 'k', "Missing `]` as FuncFParam p[]");
                state.nextToken();
            }
            while (state.getCurToken().getType() == Token.Type.LBRACK) {
                state.nextToken();
                res.appendExp(ConstExp.parse(state));
                if (state.getCurToken().getType() == Token.Type.RBRACK) {
                    state.nextToken();
                } else {
                    state.ungetToken();
                    CompError.appendError(state.getCurToken().getLineNum(), 'k', "Missing `]` as FuncFParam p[][exp]");
                    state.nextToken();
                }
            }
        }
        return res;
    }

    public static final String TYPESTR = "<FuncFParam>";
}
