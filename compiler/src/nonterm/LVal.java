package nonterm;

import compiler.ParseState;
import compiler.Token;
import symbol.CompError;

import java.util.ArrayList;
import java.util.List;

public class LVal {
    private final Token ident;
    private final List<Exp> exps = new ArrayList<>();

    private LVal(Token ident) {
        this.ident = ident;
    }

    private void appendExp(Exp exp) {
        exps.add(exp);
    }

    public static LVal parse(ParseState state) {
        var res = new LVal(state.getCurToken());
        state.nextToken();
        while (state.getCurToken().getType() == Token.Type.LBRACK) {
            state.nextToken();
            var anoExp = Exp.parse(state);
            res.appendExp(anoExp);
            if (state.getCurToken().getType() == Token.Type.RBRACK) {
                state.nextToken();
            } else {
                state.ungetToken();
                CompError.appendError(state.getCurToken().getLineNum(), 'k', "Missing `]` as LVal arr[]");
                state.nextToken();
            }
        }
        return res;
    }

    public static final String TYPESTR = "<LVal>";
}
