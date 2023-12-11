package ir.instr;

import ir.GlobalVar;
import ir.Value;
import ir.type.PointerType;

public class LoadInstr extends Instruction {
    private final Value srcPtr;

    public LoadInstr(int dstId, Value srcPtr) {
        super("T" + dstId, dstId, ((PointerType) srcPtr.getValueType()).getDeref());
        this.srcPtr = srcPtr;
    }

    public Value getSrcPtr() {
        return srcPtr;
    }

    @Override
    public void emitString(StringBuilder sb) {
        super.emitString(sb);
        sb.append('%').append(id).append(" = load ");
        type.emitString(sb); sb.append(", ");
        srcPtr.getValueType().emitString(sb); sb.append(' ');
        if (srcPtr instanceof GlobalVar) sb.append('@').append(srcPtr.getName());
        else sb.append('%').append(srcPtr.getId());
    }
}
