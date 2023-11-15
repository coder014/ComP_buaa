package nonterm;

import compiler.ParseState;

public class ConstExp {
    private final AddExp exp;

    private ConstExp(AddExp exp) {
        this.exp = exp;
    }
    public AddExp getExp() {
        return exp;
    }

    public static ConstExp parse(ParseState state) {
        return new ConstExp(AddExp.parse(state));
    }

    public static final String TYPESTR = "<ConstExp>";
}
