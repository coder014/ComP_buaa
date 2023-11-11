package nonterm;

import compiler.ParseState;
import compiler.Token;

public class UnaryOp {
    private final Token op;

    private UnaryOp(Token op) {
        this.op = op;
    }

    public static UnaryOp parse(ParseState state) {
        UnaryOp op = new UnaryOp(state.getCurToken());
                        state.nextToken();
        return op;
    }

    public static final String TYPESTR = "<UnaryOp>";
}
