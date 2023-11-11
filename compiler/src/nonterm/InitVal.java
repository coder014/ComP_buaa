package nonterm;

import compiler.ParseState;
import compiler.Token;

import java.util.ArrayList;
import java.util.List;

public class InitVal {
    private final Exp exp;
    private final List<InitVal> vals = new ArrayList<>();

    private InitVal(Exp exp) {
        this.exp = exp;
    }

    private void appendVal(InitVal val) {
        vals.add(val);
    }

    public static InitVal parse(ParseState state) {
        final InitVal res;
        if (state.getCurToken().getType() == Token.Type.LBRACE) {
                        state.nextToken();
            res = new InitVal(null);
            if (state.getCurToken().getType() != Token.Type.RBRACE) {
                res.appendVal(parse(state));
                while (state.getCurToken().getType() == Token.Type.COMMA) {
                                        state.nextToken();
                    res.appendVal(parse(state));
                }
            }
                        state.nextToken();
        } else {
            res = new InitVal(Exp.parse(state));
        }
                return res;
    }

    public static final String TYPESTR = "<InitVal>";
}
