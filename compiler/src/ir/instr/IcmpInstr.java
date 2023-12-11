package ir.instr;

import ir.IntConstant;
import ir.Value;
import ir.type.IntType;

public class IcmpInstr extends Instruction {
    private final Type cond;
    private final Value op1, op2;

    public IcmpInstr(Type cond, int dstId, Value op1, Value op2) {
        super("T" + dstId, dstId, IntType.LOGIC);
        this.op1 = op1;
        this.op2 = op2;
        this.cond = cond;
    }

    public Type getCond() {
        return cond;
    }
    public Value getOp1() {
        return op1;
    }
    public Value getOp2() {
        return op2;
    }

    @Override
    public void emitString(StringBuilder sb) {
        super.emitString(sb);
        sb.append('%').append(getId()).append(" = icmp ")
                .append(cond.toString().toLowerCase()).append(' ');
        op1.getValueType().emitString(sb); // assuming op1 and op2 has the same type
        sb.append(' ');
        if (op1 instanceof IntConstant) op1.emitString(sb);
        else sb.append('%').append(op1.getId());
        sb.append(", ");
        if (op2 instanceof IntConstant) op2.emitString(sb);
        else sb.append('%').append(op2.getId());
    }

    public enum Type {
        EQ, NE, SGT, SGE, SLT, SLE
    }
}
