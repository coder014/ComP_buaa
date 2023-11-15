package nonterm;

import compiler.ParseState;

public class Cond {
    private final LOrExp exp;

    private Cond(LOrExp exp) {
        this.exp = exp;
    }

    public LOrExp getExp() {
        return exp;
    }

    public static Cond parse(ParseState state) {
        return new Cond(LOrExp.parse(state));
    }

    public static final String TYPESTR = "<Cond>";
}
