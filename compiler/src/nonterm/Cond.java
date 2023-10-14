package nonterm;

import compiler.ParseState;

public class Cond {
    private final LOrExp exp;

    private Cond(LOrExp exp) {
        this.exp = exp;
    }

    public static Cond parse(ParseState state) {
        var res = new Cond(LOrExp.parse(state));
        System.out.println(TYPESTR);
        return res;
    }

    public static final String TYPESTR = "<Cond>";
}
