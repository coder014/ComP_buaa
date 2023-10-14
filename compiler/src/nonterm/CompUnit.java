package nonterm;

import compiler.ParseState;
import compiler.Token;

import java.util.ArrayList;
import java.util.List;

public class CompUnit {
    private final List<Decl> decls;
    private final List<FuncDef> funcDefs;
    private final MainFuncDef mainFunc;

    private CompUnit(List<Decl> decls, List<FuncDef> funcDefs, MainFuncDef mainFunc) {
        this.decls = decls;
        this.funcDefs = funcDefs;
        this.mainFunc = mainFunc;
    }

    private void appendDecl(Decl decl) {
        decls.add(decl);
    }
    private void appendFuncDef(FuncDef def) {
        funcDefs.add(def);
    }

    public static CompUnit parse(ParseState state) {
        state.nextToken();
        final var t1 = new ArrayList<Decl>();
        final var t2 = new ArrayList<FuncDef>();
        while (predictDefType(state) == DefType.DECL)
            t1.add(Decl.parse(state));
        while (predictDefType(state) == DefType.FUNCDEF)
            t2.add(FuncDef.parse(state));
        final var res = new CompUnit(t1, t2, MainFuncDef.parse(state));
        System.out.println(TYPESTR);
        return res;
    }

    private static DefType predictDefType(ParseState state) {
        Token t = state.getCurToken();
        if (t.getType() == Token.Type.CONSTTK) return DefType.DECL;
        else if (t.getType() == Token.Type.VOIDTK) return DefType.FUNCDEF;
        state.nextToken();
        t = state.getCurToken();
        if (t.getType() == Token.Type.MAINTK) {
            state.ungetToken();
            return DefType.MAINFUNCDEF;
        }
        state.nextToken();
        t = state.getCurToken();
        state.ungetToken();
        state.ungetToken();
        if (t.getType() == Token.Type.LPARENT)
            return DefType.FUNCDEF;
        return DefType.DECL;
    }

    public static final String TYPESTR = "<CompUnit>";
    private enum DefType {
        DECL, FUNCDEF, MAINFUNCDEF
    }
}
