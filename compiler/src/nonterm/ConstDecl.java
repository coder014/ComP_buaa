package nonterm;

import compiler.ParseState;
import compiler.Token;
import symbol.CompError;

import java.util.ArrayList;
import java.util.List;

public class ConstDecl {
    private final BType type;
    private final List<ConstDef> defs = new ArrayList<>();

    private ConstDecl(BType type) {
        this.type = type;
    }

    public BType getType() {
        return type;
    }
    public List<ConstDef> getDefs() {
        return defs;
    }

    private void appendDef(ConstDef def) {
        defs.add(def);
    }

    public static ConstDecl parse(ParseState state) {
        final ConstDecl res;
        state.nextToken();
        res = new ConstDecl(BType.parse(state));
        res.appendDef(ConstDef.parse(state));
        while (state.getCurToken().getType() == Token.Type.COMMA) {
            state.nextToken();
            res.appendDef(ConstDef.parse(state));
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

    public static final String TYPESTR = "<ConstDecl>";
}
