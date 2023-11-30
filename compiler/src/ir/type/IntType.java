package ir.type;

public class IntType extends ValueType {
    private final boolean isLogical;
    public static final IntType INT = new IntType(false);
    public static final IntType LOGIC = new IntType(true);

    private IntType(boolean isLogical) {
        super();
        this.isLogical = isLogical;
    }

    @Override
    public void emitString(StringBuilder sb) {
        if (isLogical) sb.append("i1");
        else sb.append("i32");
    }
}
