package nonterm;

import compiler.ParseState;
import compiler.Token;

import java.util.ArrayList;
import java.util.List;

public class Block {
    private final List<BlockItem> items;
    private final int endingLine;

    private Block(List<BlockItem> items, int endingLine) {
        this.items = items;
        this.endingLine = endingLine;
    }

    public List<BlockItem> getItems() {
        return items;
    }
    public int getEndingLineNum() {
        return endingLine;
    }

    public static Block parse(ParseState state) {
        final var items = new ArrayList<BlockItem>();
        state.nextToken();
        while (state.getCurToken().getType() != Token.Type.RBRACE) {
            items.add(BlockItem.parse(state));
        }
        final var ending = state.getCurToken().getLineNum();
        state.nextToken();
        return new Block(items, ending);
    }

    public static final String TYPESTR = "<Block>";
}
