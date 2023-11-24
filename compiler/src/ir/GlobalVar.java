package ir;

import ir.type.PointerType;
import ir.type.ValueType;

public class GlobalVar extends User {
    private final int initValue;
    // GlobalVar has pointer type

    public GlobalVar(String name, ValueType type, int initValue) {
        super(name, new PointerType(type));
        this.initValue = initValue;
    }

    @Override
    public void emitString(StringBuilder sb) {
        sb.append('@').append(name).append(" = dso_local global ");
        ((PointerType)type).getDeref().emitString(sb);
        sb.append(' ').append(initValue);
    }
}
