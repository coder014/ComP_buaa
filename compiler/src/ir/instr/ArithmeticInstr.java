package ir.instr;

import ir.IntConstant;
import ir.Value;
import ir.type.IntType;

public class ArithmeticInstr extends Instruction {
    private final Type insType;
    private final Value op1, op2;

    public ArithmeticInstr(Type insType, int dstId, Value op1, Value op2) {
        super("T" + dstId, dstId, IntType.INT);
        this.op1 = op1;
        this.op2 = op2;
        this.insType = insType;
    }

    public Type getInsType() {
        return insType;
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
        sb.append('%').append(getId()).append(" = ")
                .append(insType.toString().toLowerCase()).append(' ');
        type.emitString(sb);
        sb.append(' ');
        if (op1 instanceof IntConstant) op1.emitString(sb);
        else sb.append('%').append(op1.getId());
        sb.append(", ");
        if (op2 instanceof IntConstant) op2.emitString(sb);
        else sb.append('%').append(op2.getId());
    }

    public enum Type {
        ADD, SUB, MUL, SDIV, SREM
    }
}
