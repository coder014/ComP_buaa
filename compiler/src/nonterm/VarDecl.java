package nonterm;

import compiler.ParseState;
import compiler.Token;

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
            System.out.println(state.getCurToken());
            state.nextToken();
            res.appendDef(VarDef.parse(state));
        }
        System.out.println(state.getCurToken());
        state.nextToken();
        System.out.println(TYPESTR);
        return res;
    }

    public static final String TYPESTR = "<VarDecl>";
}
