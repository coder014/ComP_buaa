package nonterm;

import compiler.ParseState;
import compiler.Token;

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

    public static VarDef parse(ParseState state) {
        final VarDef res;
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
        if (state.getCurToken().getType() == Token.Type.ASSIGN) {
            System.out.println(state.getCurToken());
            state.nextToken();
            res = new VarDef(token, list, InitVal.parse(state));
        } else {
            res = new VarDef(token, list, null);
        }
        System.out.println(TYPESTR);
        return res;
    }

    public static final String TYPESTR = "<VarDef>";
}
