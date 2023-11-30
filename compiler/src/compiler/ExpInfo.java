package compiler;

import ir.IntConstant;
import ir.Value;
import ir.instr.Instruction;

public class ExpInfo {
    private final boolean isConst;
    private final boolean isBool;
    private final Integer value;
    private final Integer dimension;
    private final Value resIR;

    protected ExpInfo(boolean isBool, Integer value, Integer dimension) {
        this.isConst = value != null;
        this.isBool = isBool;
        this.value = value;
        this.dimension = dimension;
        this.resIR = value != null ? new IntConstant(value) : null;
    }
    protected ExpInfo(boolean isBool, Integer dimension, Instruction resIR) {
        this.isConst = false;
        this.isBool = isBool;
        this.value = null;
        this.dimension = dimension;
        this.resIR = resIR;
    }

    protected int getDimension() {
        return dimension;
    }
    protected Value getResIR() {
        return resIR;
    }
    protected boolean isConst() {
        return isConst;
    }
    protected Integer getValue() {
        return value;
    }
    protected boolean isBool() {
        return isBool;
    }
}
