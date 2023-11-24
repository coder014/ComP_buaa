package symbol;

import compiler.Utils;

import java.util.HashMap;
import java.util.Map;

public class SymbolTable {
    private static SymbolTable curTable = new SymbolTable(null);
    private final SymbolTable father;
    private final Map<String, Symbol> symbols = new HashMap<>();

    private SymbolTable(SymbolTable father) {
        this.father = father;
    }

    public static void enterNewScope() {
        curTable = new SymbolTable(curTable);
    }

    public static void exitCurrentScope() {
        if (curTable.father != null) {
            curTable = curTable.father;
        } else {
            Utils.logErrorf("Trying to exit root scope.\n");
        }
    }

    public static SymbolTable getCurrent() {
        return curTable;
    }

    public boolean appendSymbol(Symbol symbol) {
        if (symbols.containsKey(symbol.getName())) return false;
        symbols.put(symbol.getName(), symbol);
        symbol.setSymbolTable(this);
        return true;
    }

    public static Symbol getSymbolByName(String name) {
        var table = curTable;
        Symbol res = null;
        while (table != null) {
            if (table.symbols.containsKey(name)) {
                res = table.symbols.get(name);
                break;
            }
            table = table.father;
        }
        return res;
    }

    public boolean isRoot() {
        return father == null;
    }
}
