package nonterm;

import compiler.ParseState;
import compiler.Token;
import symbol.CompError;

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

    public Token getType() {
        return type.getfType();
    }
    public Token getIdent() {
        return ident;
    }
    public FuncFParams getParams() {
        return params;
    }
    public Block getBlock() {
        return block;
    }

    public static FuncDef parse(ParseState state) {
        final var t1 = FuncType.parse(state);
        final var t2 = state.getCurToken();
        final FuncFParams t3;
        final FuncDef res;
        state.nextToken();
        state.nextToken();
        if (state.getCurToken().getType() == Token.Type.LBRACE) {
            t3 = null;
            state.ungetToken();
            CompError.appendError(state.getCurToken().getLineNum(), 'j', "Missing `)` as FuncDef()");
            state.nextToken();
        } else if (state.getCurToken().getType() != Token.Type.RPARENT) {
            t3 = FuncFParams.parse(state);
            if (state.getCurToken().getType() == Token.Type.RPARENT) {
                state.nextToken();
            } else {
                state.ungetToken();
                CompError.appendError(state.getCurToken().getLineNum(), 'j', "Missing `)` as FuncDef(params)");
                state.nextToken();
            }
        } else {
            t3 = null;
            state.nextToken();
        }
        res = new FuncDef(t1, t2, t3, Block.parse(state));
        return res;
    }

    public static final String TYPESTR = "<FuncDef>";
}
