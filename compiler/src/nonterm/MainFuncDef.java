package nonterm;

import compiler.ParseState;
import compiler.Token;
import symbol.CompError;

public class MainFuncDef {
    private final Block block;

    private MainFuncDef(Block block) {
        this.block = block;
    }

    public Block getBlock() {
        return block;
    }

    public static MainFuncDef parse(ParseState state) {
        state.nextToken();
        state.nextToken();
        state.nextToken();
        if (state.getCurToken().getType() == Token.Type.RPARENT) {
            state.nextToken();
        } else {
            state.ungetToken();
            CompError.appendError(state.getCurToken().getLineNum(), 'j', "Missing `)` as main()");
            state.nextToken();
        }
        return new MainFuncDef(Block.parse(state));
    }

    public static final String TYPESTR = "<MainFuncDef>";
}
