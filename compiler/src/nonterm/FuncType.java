package nonterm;

import compiler.ParseState;
import compiler.Token;

public class FuncType {
    private final Token fType;

    private FuncType(Token fType) {
        this.fType = fType;
    }

    public static FuncType parse(ParseState state) {
        final var res = new FuncType(state.getCurToken());
        System.out.println(state.getCurToken());
        state.nextToken();
        System.out.println(TYPESTR);
        return res;
    }

    public static final String TYPESTR = "<FuncType>";
}
