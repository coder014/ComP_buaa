package nonterm;

import compiler.ParseState;
import compiler.Token;

import java.util.ArrayList;
import java.util.List;

public class FuncRParams {
    private final List<Exp> exps;

    private FuncRParams(Exp exp) {
        exps = new ArrayList<>();
        exps.add(exp);
    }

    private void appendExp(Exp exp) {
        exps.add(exp);
    }

    public static FuncRParams parse(ParseState state) {
        final FuncRParams res = new FuncRParams(Exp.parse(state));
        while (state.getCurToken().getType() == Token.Type.COMMA) {
            System.out.println(state.getCurToken());
            state.nextToken();
            res.appendExp(Exp.parse(state));
        }
        System.out.println(TYPESTR);
        return res;
    }

    public static final String TYPESTR = "<FuncRParams>";
}
