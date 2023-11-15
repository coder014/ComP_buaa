package nonterm;

import compiler.ParseState;

public class Exp {
    private final AddExp exp;

    private Exp(AddExp exp) {
        this.exp = exp;
    }

    public AddExp getExp() {
        return exp;
    }

    public static Exp parse(ParseState state) {
        return new Exp(AddExp.parse(state));
    }

    public static final String TYPESTR = "<Exp>";
}
