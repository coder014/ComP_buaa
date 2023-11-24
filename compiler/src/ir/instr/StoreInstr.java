package ir.instr;

import ir.GlobalVar;
import ir.IntConstant;
import ir.Value;
import ir.type.VoidType;

public class StoreInstr extends Instruction {
    private final Value storeValue;
    private final Value dstPtr;

    public StoreInstr(Value storeValue, Value dstPtr) {
        super(VoidType.INSTANCE);
        this.storeValue = storeValue;
        this.dstPtr = dstPtr;
    }

    @Override
    public void emitString(StringBuilder sb) {
        super.emitString(sb);
        sb.append("store "); storeValue.getValueType().emitString(sb); sb.append(' ');
        if (storeValue instanceof IntConstant) storeValue.emitString(sb);
        else sb.append('%').append(storeValue.getId());
        sb.append(", ");
        dstPtr.getValueType().emitString(sb); sb.append(' ');
        if (dstPtr instanceof GlobalVar) sb.append('@').append(dstPtr.getName());
        else sb.append('%').append(dstPtr.getId());
    }
}
