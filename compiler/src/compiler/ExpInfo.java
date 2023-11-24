package compiler;

import ir.IntConstant;
import ir.Value;
import ir.instr.Instruction;

public class ExpInfo {
    private final boolean isConst;
    private final boolean isBool;
    private final Integer value;
    private final Boolean bool;
    private final Integer dimension;
    private final Value resIR;

    protected ExpInfo(Integer value, Integer dimension) {
        this.isConst = value != null;
        this.isBool = false;
        this.value = value;
        this.dimension = dimension;
        this.bool = false;
        this.resIR = value != null ? new IntConstant(value) : null;
    }
    protected ExpInfo(Integer dimension, Instruction resIR) {
        this.isConst = false;
        this.isBool = false;
        this.value = null;
        this.dimension = dimension;
        this.bool = false;
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
}
