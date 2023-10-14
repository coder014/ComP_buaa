package nonterm;

import compiler.ParseState;
import compiler.Token;

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
        System.out.println(state.getCurToken());
        state.nextToken();
        while (state.getCurToken().getType() == Token.Type.LBRACK) {
            System.out.println(state.getCurToken());
            state.nextToken();
            var anoExp = Exp.parse(state);
            System.out.println(state.getCurToken());
            state.nextToken();
            res.appendExp(anoExp);
        }
        System.out.println(TYPESTR);
        return res;
    }

    public static final String TYPESTR = "<LVal>";
}
