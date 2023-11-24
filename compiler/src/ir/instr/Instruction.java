package ir.instr;

import ir.User;
import ir.type.ValueType;

public class Instruction extends User {
    // String name stands for destination name
    // ValueType type stands for destination value type
    // int id stands for destination id
    protected Instruction(String name, ValueType type) {
        super(name, type);
    }
    protected Instruction(int id, ValueType type) {
        super(id, type);
    }
    protected Instruction(ValueType type) {
        super(type);
    }
    protected Instruction(String s, int id, ValueType type) {
        super(s, id, type);
    }

    @Override
    public void emitString(StringBuilder sb) {
        sb.append('\t'); // prettier
    }
}
