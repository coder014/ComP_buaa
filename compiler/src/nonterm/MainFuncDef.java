package nonterm;

import compiler.ParseState;

public class MainFuncDef {
    private final Block block;

    private MainFuncDef(Block block) {
        this.block = block;
    }

    public static MainFuncDef parse(ParseState state) {
        System.out.println(state.getCurToken());
        state.nextToken();
        System.out.println(state.getCurToken());
        state.nextToken();
        System.out.println(state.getCurToken());
        state.nextToken();
        System.out.println(state.getCurToken());
        state.nextToken();
        final MainFuncDef res = new MainFuncDef(Block.parse(state));
        System.out.println(TYPESTR);
        return res;
    }

    public static final String TYPESTR = "<MainFuncDef>";
}
