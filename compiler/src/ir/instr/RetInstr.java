package ir.instr;

import ir.IntConstant;
import ir.Value;
import ir.type.VoidType;

public class RetInstr extends Instruction {
    private final Value retVal;

    public RetInstr() {
        super(VoidType.INSTANCE);
        this.retVal = null;
    }

    public RetInstr(Value retVal) {
        super(VoidType.INSTANCE);
        this.retVal = retVal;
    }

    @Override
    public void emitString(StringBuilder sb) {
        super.emitString(sb);
        sb.append("ret ");
        if (this.retVal == null)
            sb.append("void");
        else {
            retVal.getValueType().emitString(sb);
            sb.append(' ');
            if (retVal instanceof IntConstant) retVal.emitString(sb);
            else sb.append('%').append(retVal.getId());
        }
    }
}
