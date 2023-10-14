package nonterm;

import compiler.ParseState;
import compiler.Token;

public class BType {
    private final Token intType;

    private BType(Token intType) {
        this.intType = intType;
    }

    public static BType parse(ParseState state) {
        var res = new BType(state.getCurToken());
        System.out.println(state.getCurToken());
        state.nextToken();
        return res;
    }

    public static final String TYPESTR = "<BType>";
}
