package ir.instr;

import ir.type.PointerType;
import ir.type.ValueType;

public class AllocaInstr extends Instruction {
    private final int numElements;

    public AllocaInstr(int dstId, ValueType derefType) { // for basic
        super("PTR" + dstId, dstId, new PointerType(derefType));
        numElements = 1;
    }
    public AllocaInstr(int dstId, ValueType derefType, int numElements) { // for basic
        super("PTR" + dstId, dstId, new PointerType(derefType));
        this.numElements = numElements;
    }

    public ValueType getDerefType() {
        return ((PointerType)type).getDeref();
    }

    @Override
    public void emitString(StringBuilder sb) {
        super.emitString(sb);
        sb.append('%').append(id).append(" = alloca ");
        ((PointerType)type).getDeref().emitString(sb);
    }
}
