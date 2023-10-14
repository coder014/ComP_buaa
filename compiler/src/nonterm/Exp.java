package nonterm;

import compiler.ParseState;

public class Exp {
    private final AddExp exp;

    private Exp(AddExp exp) {
        this.exp = exp;
    }

    public static Exp parse(ParseState state) {
        var res = new Exp(AddExp.parse(state));
        System.out.println(TYPESTR);
        return res;
    }

    public static final String TYPESTR = "<Exp>";
}
