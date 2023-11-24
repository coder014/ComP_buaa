package ir;

import ir.type.ValueType;

public class FuncParam extends Value {
    public FuncParam(int id, ValueType valueType) {
        super("FP" + id, id, valueType);
    }

    @Override
    public void emitString(StringBuilder sb) {

    }
}
