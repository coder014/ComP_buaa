package nonterm;

import compiler.ParseState;
import compiler.Token;

import java.util.ArrayList;
import java.util.List;

public class ConstInitVal {
    private final ConstExp exp;
    private final List<ConstInitVal> vals = new ArrayList<>();

    private ConstInitVal(ConstExp exp) {
        this.exp = exp;
    }

    private void appendVal(ConstInitVal val) {
        vals.add(val);
    }

    public static ConstInitVal parse(ParseState state) {
        final ConstInitVal res;
        if (state.getCurToken().getType() == Token.Type.LBRACE) {
            state.nextToken();
            res = new ConstInitVal(null);
            if (state.getCurToken().getType() != Token.Type.RBRACE) {
                res.appendVal(parse(state));
                while (state.getCurToken().getType() == Token.Type.COMMA) {
                    state.nextToken();
                    res.appendVal(parse(state));
                }
            }
            state.nextToken();
        } else {
            res = new ConstInitVal(ConstExp.parse(state));
        }
        return res;
    }

    public static final String TYPESTR = "<ConstInitVal>";
}
