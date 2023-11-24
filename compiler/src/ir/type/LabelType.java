package ir.type;

public class LabelType extends ValueType {
    public static final LabelType INSTANCE = new LabelType();

    private LabelType() {
        super();
    }

    @Override
    public void emitString(StringBuilder sb) {

    }
}
