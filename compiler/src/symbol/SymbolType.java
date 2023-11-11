package symbol;

import compiler.Token;

import java.util.List;

public class SymbolType {
    private final Category category;
    private final Token.Type bType; // INTTK || VOIDTK
    private final List<Integer> arrSizes;
    private final boolean isConst;
    private final List<SymbolType> params;

    public enum Category {
        BASIC, ARRAY, FUNC
    }

    public SymbolType(boolean isConst) { // BASIC
        this.category = Category.BASIC;
        this.bType = Token.Type.INTTK;
        this.arrSizes = null;
        this.isConst = isConst;
        this.params = null;
    }

    public SymbolType(boolean isConst, List<Integer> arrSizes) { // ARRAY
        this.category = Category.ARRAY;
        this.bType = Token.Type.INTTK;
        this.arrSizes = arrSizes;
        this.isConst = isConst;
        this.params = null;
    }

    public SymbolType(Token.Type bType, List<SymbolType> params) { // FUNC
        this.category = Category.FUNC;
        this.bType = bType;
        this.arrSizes = null;
        this.isConst = false;
        this.params = params;
    }
}