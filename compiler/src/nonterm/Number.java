package nonterm;

import compiler.ParseState;
import compiler.Token;

public class Number {
    private final Token intConst;

    private Number(Token intConst) {
        this.intConst = intConst;
    }

    public int getIntValue() {
        return Integer.parseInt(intConst.getValue());
    }

    public static Number parse(ParseState state) {
        var res = new Number(state.getCurToken());
        state.nextToken();
        return res;
    }

    public static final String TYPESTR = "<Number>";
}
