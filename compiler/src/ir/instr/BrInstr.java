package ir.instr;

import ir.BasicBlock;
import ir.Value;
import ir.type.VoidType;

public class BrInstr extends Instruction {
    private final Value cond; // null indicates unconditional branch
    private final BasicBlock tBranch, fBranch; // if unconditional, tBranch is for destination

    public BrInstr(Value cond, BasicBlock tBranch, BasicBlock fBranch) {
        super(VoidType.INSTANCE);
        this.cond = cond;
        this.tBranch = tBranch;
        this.fBranch = fBranch;
    }
    public BrInstr(BasicBlock dest) {
        super(VoidType.INSTANCE);
        this.cond = null;
        this.tBranch = dest;
        this.fBranch = null;
    }

    @Override
    public void emitString(StringBuilder sb) {
        super.emitString(sb);
        sb.append("br ");
        if (this.cond != null)
            sb.append("i1 %").append(cond.getId())
                    .append(", label %").append(tBranch.getId())
                    .append(", label %").append(fBranch.getId());
        else sb.append("label %").append(tBranch.getId());
    }
}
