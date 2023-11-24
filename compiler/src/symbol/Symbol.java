package symbol;

import ir.Value;

import java.util.List;

public class Symbol {
    private final String name;
    private final SymbolType type;
    private SymbolTable symTable;
    private Integer value;
    private List<Integer> valArr1;
    private List<List<Integer>> valArr2;
    private Value irValue;

    public Symbol(String name, SymbolType type) {
        this.name = name;
        this.type = type;
        this.value = null;
        this.valArr1 = null;
        this.valArr2 = null;
    }

    protected void setSymbolTable(SymbolTable symTable) {
        this.symTable = symTable;
    }
    public boolean isGlobal() {
        return symTable.isRoot();
    }
    public String getName() {
        return name;
    }
    public SymbolType getType() {
        return type;
    }
    public SymbolTable getSymTable() {
        return symTable;
    }
    public void setSymTable(SymbolTable symTable) {
        this.symTable = symTable;
    }

    public Value getIrValue() {
        return irValue;
    }
    public void setIrValue(Value irValue) {
        this.irValue = irValue;
    }
    public Integer getIntValue() {
        return value;
    }
    public void setIntValue(Integer value) {
        this.value = value;
    }
    public List<Integer> getValArr1() {
        return valArr1;
    }
    public void setValArr1(List<Integer> valArr1) {
        this.valArr1 = valArr1;
    }
    public List<List<Integer>> getValArr2() {
        return valArr2;
    }
    public void setValArr2(List<List<Integer>> valArr2) {
        this.valArr2 = valArr2;
    }
}
