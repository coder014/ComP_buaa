package ir.type;

public class PointerType extends ValueType {
    private final ValueType derefType;

    public PointerType(ValueType derefType) {
        super();
        this.derefType = derefType;
    }

    public ValueType getDeref() {
        return derefType;
    }

    @Override
    public void emitString(StringBuilder sb) {
        derefType.emitString(sb);
        sb.append('*');
    }
}
