package nonterm;

import compiler.ParseState;

public class ConstExp {
    private final AddExp exp;

    private ConstExp(AddExp exp) {
        this.exp = exp;
    }

    public static ConstExp parse(ParseState state) {
        var res = new ConstExp(AddExp.parse(state));
        System.out.println(TYPESTR);
        return res;
    }

    public static final String TYPESTR = "<ConstExp>";
}
