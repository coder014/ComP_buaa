package nonterm;

import compiler.ParseState;
import compiler.Token;
import symbol.CompError;

import java.util.ArrayList;
import java.util.List;

public class VarDecl {
    private final BType type;
    private final List<VarDef> defs = new ArrayList<>();

    private VarDecl(BType type) {
        this.type = type;
    }

    private void appendDef(VarDef def) {
        defs.add(def);
    }

    public static VarDecl parse(ParseState state) {
        final VarDecl res = new VarDecl(BType.parse(state));
        res.appendDef(VarDef.parse(state));
        while (state.getCurToken().getType() == Token.Type.COMMA) {
            state.nextToken();
            res.appendDef(VarDef.parse(state));
        }
        if (state.getCurToken().getType() == Token.Type.SEMICN) {
            state.nextToken();
        } else {
            state.ungetToken();
            final var lastToken = state.getCurToken();
            state.nextToken();
            CompError.appendError(lastToken.getLineNum(), 'i', "Missing `;` after token " + lastToken.getValue());
        }
        return res;
    }

    public static final String TYPESTR = "<VarDecl>";
}
