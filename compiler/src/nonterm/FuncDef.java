package nonterm;

import compiler.ParseState;
import compiler.Token;

public class FuncDef {
    private final FuncType type;
    private final Token ident;
    private final FuncFParams params;
    private final Block block;

    private FuncDef(FuncType type, Token ident, FuncFParams params, Block block) {
        this.type = type;
        this.ident = ident;
        this.params = params;
        this.block = block;
    }

    public static FuncDef parse(ParseState state) {
        final var t1 = FuncType.parse(state);
        final var t2 = state.getCurToken();
        final FuncFParams t3;
        final FuncDef res;
        System.out.println(t2);
        state.nextToken();
        System.out.println(state.getCurToken());
        state.nextToken();
        if (state.getCurToken().getType() != Token.Type.RPARENT) {
            t3 = FuncFParams.parse(state);
        } else t3 = null;
        System.out.println(state.getCurToken());
        state.nextToken();
        res = new FuncDef(t1, t2, t3, Block.parse(state));
        System.out.println(TYPESTR);
        return res;
    }

    public static final String TYPESTR = "<FuncDef>";
}
