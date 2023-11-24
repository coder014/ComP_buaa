package ir;

import ir.type.IntType;

public class IntConstant extends Value {
    private final int value;

    public IntConstant(int value) {
        super(IntType.INT);
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    @Override
    public void emitString(StringBuilder sb) {
        sb.append(value);
    }
}
