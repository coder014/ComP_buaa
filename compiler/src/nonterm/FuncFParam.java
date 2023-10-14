package nonterm;

import compiler.ParseState;
import compiler.Token;

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
        System.out.println(state.getCurToken());
        state.nextToken();
        if (state.getCurToken().getType() == Token.Type.LBRACK) {
            System.out.println(state.getCurToken());
            state.nextToken();
            res.appendExp(null);
            System.out.println(state.getCurToken());
            state.nextToken();
            while (state.getCurToken().getType() == Token.Type.LBRACK) {
                System.out.println(state.getCurToken());
                state.nextToken();
                res.appendExp(ConstExp.parse(state));
                System.out.println(state.getCurToken());
                state.nextToken();
            }
        }
        System.out.println(TYPESTR);
        return res;
    }

    public static final String TYPESTR = "<FuncFParam>";
}
