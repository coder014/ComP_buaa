package ir;

import ir.type.ValueType;

public class User extends Value {
    protected User(String name, int id, ValueType type) {
        super(name, id, type);
    }
    protected User(String name, ValueType type) {
        super(name, type);
    }
    protected User(int id, ValueType type) {
        super(id, type);
    }
    protected User(ValueType type) {
        super(type);
    }

    @Override
    public void emitString(StringBuilder sb) {
        sb.append("<Abstract User to String not supported>");
    }
}
