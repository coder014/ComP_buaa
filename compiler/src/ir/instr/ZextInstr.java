package ir.instr;

import ir.IntConstant;
import ir.Value;
import ir.type.ValueType;

public class ZextInstr extends Instruction {
    private final Value toExt;

    public ZextInstr(ValueType toType, int dstId, Value toExt) {
        super("T" + dstId, dstId, toType);
        this.toExt = toExt;
    }

    @Override
    public void emitString(StringBuilder sb) {
        super.emitString(sb);
        sb.append('%').append(getId()).append(" = zext ");
        toExt.getValueType().emitString(sb);
        sb.append(" %").append(toExt.getId()).append(" to ");
        type.emitString(sb);
    }
}
