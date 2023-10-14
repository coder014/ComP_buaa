package nonterm;

import compiler.ParseState;
import compiler.Token;

public class Decl {
    private final ConstDecl constDecl;
    private final VarDecl varDecl;

    private Decl(ConstDecl constDecl) {
        this.constDecl = constDecl;
        this.varDecl = null;
    }
    private Decl(VarDecl varDecl) {
        this.constDecl = null;
        this.varDecl = varDecl;
    }

    public static Decl parse(ParseState state) {
        final Decl res;
        if (state.getCurToken().getType() == Token.Type.CONSTTK) {
            res = new Decl(ConstDecl.parse(state));
        } else {
            res = new Decl(VarDecl.parse(state));
        }
        return res;
    }

    public static final String TYPESTR = "<Decl>";
}
