package ir.type;

public class ArrayType extends ValueType {
    private final ValueType eleType;
    private final int eleSize;

    public ArrayType(int eleSize, ValueType eleType) {
        super();
        this.eleSize = eleSize;
        this.eleType = eleType;
    }

    public ValueType getElementType() {
        return eleType;
    }

    public int getElementSize() {
        return eleSize;
    }

    @Override
    public void emitString(StringBuilder sb) {
        sb.append('[').append(eleSize).append(" x ");
        eleType.emitString(sb);
        sb.append(']');
    }
}
