package nonterm;

import compiler.ParseState;
import compiler.Token;

import java.util.ArrayList;
import java.util.List;

public class ConstDecl {
    private final BType type;
    private final List<ConstDef> defs = new ArrayList<>();

    private ConstDecl(BType type) {
        this.type = type;
    }

    private void appendDef(ConstDef def) {
        defs.add(def);
    }

    public static ConstDecl parse(ParseState state) {
        final ConstDecl res;
        System.out.println(state.getCurToken());
        state.nextToken();
        res = new ConstDecl(BType.parse(state));
        res.appendDef(ConstDef.parse(state));
        while (state.getCurToken().getType() == Token.Type.COMMA) {
            System.out.println(state.getCurToken());
            state.nextToken();
            res.appendDef(ConstDef.parse(state));
        }
        System.out.println(state.getCurToken());
        state.nextToken();
        System.out.println(TYPESTR);
        return res;
    }

    public static final String TYPESTR = "<ConstDecl>";
}
