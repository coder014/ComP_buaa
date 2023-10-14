package nonterm;

import compiler.ParseState;
import compiler.Token;

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

    public static ConstDef parse(ParseState state) {
        final Token token = state.getCurToken();
        final List<ConstExp> list = new ArrayList<>();
        System.out.println(token);
        state.nextToken();
        while (state.getCurToken().getType() == Token.Type.LBRACK) {
            System.out.println(state.getCurToken());
            state.nextToken();
            list.add(ConstExp.parse(state));
            System.out.println(state.getCurToken());
            state.nextToken();
        }
        System.out.println(state.getCurToken());
        state.nextToken();
        final ConstDef res = new ConstDef(token, list, ConstInitVal.parse(state));
        System.out.println(TYPESTR);
        return res;
    }

    public static final String TYPESTR = "<ConstDef>";
}
