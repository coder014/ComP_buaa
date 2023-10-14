package nonterm;

import compiler.ParseState;
import compiler.Token;

public class BlockItem {
    private final Decl decl;
    private final Stmt stmt;

    private BlockItem(Decl decl) {
        this.decl = decl;
        this.stmt = null;
    }
    private BlockItem(Stmt stmt) {
        this.stmt = stmt;
        this.decl = null;
    }

    public static BlockItem parse(ParseState state) {
        if (state.getCurToken().getType() == Token.Type.INTTK
                || state.getCurToken().getType() == Token.Type.CONSTTK) {
            return new BlockItem(Decl.parse(state));
        } else {
            return new BlockItem(Stmt.parse(state));
        }
    }

    public static final String TYPESTR = "<BlockItem>";
}
