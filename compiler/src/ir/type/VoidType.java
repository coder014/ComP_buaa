package ir.type;

public class VoidType extends ValueType {
    public static final VoidType INSTANCE = new VoidType();

    private VoidType() {
        super();
    }

    @Override
    public void emitString(StringBuilder sb) {
        sb.append("void");
    }
}
