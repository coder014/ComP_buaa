package nonterm;

import compiler.ParseState;
import compiler.Token;

import java.util.ArrayList;
import java.util.List;

public class Block {
    private final List<BlockItem> items = new ArrayList<>();

    private Block() {}

    private void appendItem(BlockItem item) {
        items.add(item);
    }

    public static Block parse(ParseState state) {
        System.out.println(state.getCurToken());
        state.nextToken();
        final var res = new Block();
        while (state.getCurToken().getType() != Token.Type.RBRACE) {
            res.appendItem(BlockItem.parse(state));
        }
        System.out.println(state.getCurToken());
        state.nextToken();
        System.out.println(TYPESTR);
        return res;
    }

    public static final String TYPESTR = "<Block>";
}
