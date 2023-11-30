package ir;

import ir.type.ValueType;

public class Value implements IRPrintable {
    protected String name;
    protected final ValueType type;
    protected int id;

    protected Value(String name, int id, ValueType type) {
        this.name = name;
        this.type = type;
        this.id = id;
    }
    protected Value(int id, ValueType type) {
        this("V" + id, id, type);
    }
    protected Value(String name, ValueType type) {
        this(name, -1, type);
    }
    protected Value(ValueType type) {
        this(null, type);
    }

    public void emitString(StringBuilder sb) {
        sb.append("<Abstract Value to String not supported>");
    }

    public String getName() {
        return name;
    }
    public int getId() {
        return id;
    }
    public ValueType getValueType() {
        return type;
    }

    public void setId(int id) {
        this.id = id;
        this.name = "V" + id;
    }
}
