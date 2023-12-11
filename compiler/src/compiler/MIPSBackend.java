package compiler;

import ir.BasicBlock;
import ir.Function;
import ir.GlobalVar;
import ir.IntConstant;
import ir.Module;
import ir.instr.*;
import ir.type.ArrayType;
import ir.type.IntType;
import ir.type.PointerType;
import ir.type.ValueType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MIPSBackend {
    private static final List<String> mipsAsm = new ArrayList<>();
    private static String curFuncName;
    private static int curFuncParamCount;
    private static Map<Integer, Integer> curVRegPos;
    private static Map<Integer, Integer> curLocalVarPos;
    private static int curSPOffset;
    public static void parseLLVM(Module module) {
        mipsAsm.add(".data");
        for (final var gv : module.getGlobalVars()) {
            parseGlobalVar(gv);
        }
        mipsAsm.add(".text");
        mipsAsm.add("jal func_main");
        mipsAsm.add("ori $v0, $0, 10");
        mipsAsm.add("syscall");
        for (final var func : module.getFunctions()) {
            parseFunction(func);
        }
    }

    private static void parseGlobalVar(GlobalVar gv) {
        mipsAsm.add("g_" + gv.getName() + ":");
        if (gv.getInitValue() != null) { // int
            mipsAsm.add(".word " + gv.getInitValue());
        } else if (gv.getArray1() != null) { // 1-array
            final var init = gv.getArray1();
            final var size = 4
                    * ((ArrayType)((PointerType)gv.getValueType()).getDeref()).getElementSize();
            if (init.isEmpty()) mipsAsm.add(".space " + size);
            else mipsAsm.add(".word " +
                        init.stream().map(String::valueOf).collect(Collectors.joining(", ")));
        } else { // 2-array
            final var init = gv.getArray2();
            final var size2 = 4
                    * ((ArrayType)((ArrayType)((PointerType)gv.getValueType()).getDeref()).getElementType()).getElementSize();
            final var size = size2
                    * ((ArrayType)((PointerType)gv.getValueType()).getDeref()).getElementSize();
            if (init.isEmpty())  mipsAsm.add(".space " + size);
            else for (final var init2 : init) {
                if (init2.isEmpty()) mipsAsm.add(".space " + size2);
                else mipsAsm.add(".word " +
                        init2.stream().map(String::valueOf).collect(Collectors.joining(", ")));
            }
        }
    }

    private static void parseFunction(Function func) {
        curFuncName = func.getName();
        curFuncParamCount = func.getParams().size();
        curVRegPos = new HashMap<>();
        curLocalVarPos = new HashMap<>();
        curSPOffset = -4;
        mipsAsm.add("func_" + curFuncName + ":");
        mipsAsm.add("sw $ra, -4($sp)");
        for (final var block : func.getBlocks()) {
            parseBasicBlock(block);
        }
    }

    private static void parseBasicBlock(BasicBlock block) {
        if (block.isEmpty()) return;
        mipsAsm.add("func_" + curFuncName + "_block_" + block.getId() + ":");
        for(final var instr : block.getInstructions()) {
            if (instr instanceof AllocaInstr) parseAlloca((AllocaInstr) instr);
            else if (instr instanceof ArithmeticInstr) parseArithmetic((ArithmeticInstr) instr);
            else if (instr instanceof BrInstr) parseBr((BrInstr) instr);
            else if (instr instanceof CallInstr) parseCall((CallInstr) instr);
            else if (instr instanceof GetelementptrInstr) parseGetelementptr((GetelementptrInstr) instr);
            else if (instr instanceof IcmpInstr) parseIcmp((IcmpInstr) instr);
            else if (instr instanceof LoadInstr) parseLoad((LoadInstr) instr);
            else if (instr instanceof RetInstr) parseRet((RetInstr) instr);
            else if (instr instanceof StoreInstr) parseStore((StoreInstr) instr);
            else /* (instr instanceof ZextInstr) */ parseZext((ZextInstr) instr);
        }
    }

    private static void parseAlloca(AllocaInstr instr) {
        final var size = sizeOfIRType(instr.getDerefType());
        curSPOffset -= size;
        curLocalVarPos.put(instr.getId(), curSPOffset);
    }
    private static int sizeOfIRType(ValueType type) {
        if (type instanceof PointerType || type instanceof IntType) return 4;
        if (type instanceof ArrayType) {
            final var arrType = (ArrayType) type;
            return arrType.getElementSize() * sizeOfIRType(arrType.getElementType());
        }
        return 0;
    }

    private static void parseArithmetic(ArithmeticInstr instr) {
        switch (instr.getInsType()) {
            case ADD:
                if (!(instr.getOp1() instanceof IntConstant) && !(instr.getOp2() instanceof IntConstant)) {
                    mipsAsm.add("lw $t1, " + curVRegPos.get(instr.getOp1().getId()) + "($sp)");
                    mipsAsm.add("lw $t2, " + curVRegPos.get(instr.getOp2().getId()) + "($sp)");
                    mipsAsm.add("addu $t0, $t1, $t2");
                } else if (instr.getOp1() instanceof IntConstant) {
                    mipsAsm.add("lw $t1, " + curVRegPos.get(instr.getOp2().getId()) + "($sp)");
                    mipsAsm.add("addiu $t0, $t1, " + ((IntConstant)instr.getOp1()).getValue());
                } else {
                    mipsAsm.add("lw $t1, " + curVRegPos.get(instr.getOp1().getId()) + "($sp)");
                    mipsAsm.add("addiu $t0, $t1, " + ((IntConstant)instr.getOp2()).getValue());
                }
                curSPOffset -= 4;
                curVRegPos.put(instr.getId(), curSPOffset);
                mipsAsm.add("sw $t0, " + curSPOffset + "($sp)");
                break;
            case SUB:
                if (!(instr.getOp1() instanceof IntConstant) && !(instr.getOp2() instanceof IntConstant)) {
                    mipsAsm.add("lw $t1, " + curVRegPos.get(instr.getOp1().getId()) + "($sp)");
                    mipsAsm.add("lw $t2, " + curVRegPos.get(instr.getOp2().getId()) + "($sp)");
                    mipsAsm.add("subu $t0, $t1, $t2");
                } else if (instr.getOp1() instanceof IntConstant) {
                    mipsAsm.add("li $t1, " + ((IntConstant)instr.getOp1()).getValue());
                    mipsAsm.add("lw $t2, " + curVRegPos.get(instr.getOp2().getId()) + "($sp)");
                    mipsAsm.add("subu $t0, $t1, $t2");
                } else {
                    final var v2 = ((IntConstant)instr.getOp2()).getValue();
                    mipsAsm.add("lw $t1, " + curVRegPos.get(instr.getOp1().getId()) + "($sp)");
                    if (v2 >= -32767 && v2 <= 32767) mipsAsm.add("addiu $t0, $t1, " + (-v2));
                    else mipsAsm.add("subiu $t0, $t1, " + v2);
                }
                curSPOffset -= 4;
                curVRegPos.put(instr.getId(), curSPOffset);
                mipsAsm.add("sw $t0, " + curSPOffset + "($sp)");
                break;
            case MUL:
                if (!(instr.getOp1() instanceof IntConstant) && !(instr.getOp2() instanceof IntConstant)) {
                    mipsAsm.add("lw $t1, " + curVRegPos.get(instr.getOp1().getId()) + "($sp)");
                    mipsAsm.add("lw $t2, " + curVRegPos.get(instr.getOp2().getId()) + "($sp)");
                    mipsAsm.add("mul $t0, $t1, $t2");
                } else if (instr.getOp1() instanceof IntConstant) {
                    mipsAsm.add("lw $t1, " + curVRegPos.get(instr.getOp2().getId()) + "($sp)");
                    mipsAsm.add("mul $t0, $t1, " + ((IntConstant)instr.getOp1()).getValue());
                } else {
                    mipsAsm.add("lw $t1, " + curVRegPos.get(instr.getOp1().getId()) + "($sp)");
                    mipsAsm.add("mul $t0, $t1, " + ((IntConstant)instr.getOp2()).getValue());
                }
                curSPOffset -= 4;
                curVRegPos.put(instr.getId(), curSPOffset);
                mipsAsm.add("sw $t0, " + curSPOffset + "($sp)");
                break;
            case SDIV:
                if (!(instr.getOp1() instanceof IntConstant) && !(instr.getOp2() instanceof IntConstant)) {
                    mipsAsm.add("lw $t1, " + curVRegPos.get(instr.getOp1().getId()) + "($sp)");
                    mipsAsm.add("lw $t2, " + curVRegPos.get(instr.getOp2().getId()) + "($sp)");
                    mipsAsm.add("div $t0, $t1, $t2");
                } else if (instr.getOp1() instanceof IntConstant) {
                    mipsAsm.add("li $t1, " + ((IntConstant)instr.getOp1()).getValue());
                    mipsAsm.add("lw $t2, " + curVRegPos.get(instr.getOp2().getId()) + "($sp)");
                    mipsAsm.add("div $t0, $t1, $t2");
                } else {
                    mipsAsm.add("lw $t1, " + curVRegPos.get(instr.getOp1().getId()) + "($sp)");
                    mipsAsm.add("div $t0, $t1, " + ((IntConstant)instr.getOp2()).getValue());
                }
                curSPOffset -= 4;
                curVRegPos.put(instr.getId(), curSPOffset);
                mipsAsm.add("sw $t0, " + curSPOffset + "($sp)");
                break;
            case SREM:
                if (!(instr.getOp1() instanceof IntConstant) && !(instr.getOp2() instanceof IntConstant)) {
                    mipsAsm.add("lw $t1, " + curVRegPos.get(instr.getOp1().getId()) + "($sp)");
                    mipsAsm.add("lw $t2, " + curVRegPos.get(instr.getOp2().getId()) + "($sp)");
                    mipsAsm.add("rem $t0, $t1, $t2");
                } else if (instr.getOp1() instanceof IntConstant) {
                    mipsAsm.add("li $t1, " + ((IntConstant)instr.getOp1()).getValue());
                    mipsAsm.add("lw $t2, " + curVRegPos.get(instr.getOp2().getId()) + "($sp)");
                    mipsAsm.add("rem $t0, $t1, $t2");
                } else {
                    mipsAsm.add("lw $t1, " + curVRegPos.get(instr.getOp1().getId()) + "($sp)");
                    mipsAsm.add("rem $t0, $t1, " + ((IntConstant)instr.getOp2()).getValue());
                }
                curSPOffset -= 4;
                curVRegPos.put(instr.getId(), curSPOffset);
                mipsAsm.add("sw $t0, " + curSPOffset + "($sp)");
                break;
        }
    }

    private static void parseBr(BrInstr instr) {
        if (instr.getCond() == null) {
            mipsAsm.add("j func_" + curFuncName + "_block_" + instr.gettBranch().getId());
        } else {
            mipsAsm.add("lw $t0, " + curVRegPos.get(instr.getCond().getId()) + "($sp)");
            mipsAsm.add("beq $t0, $0, func_" + curFuncName + "_block_" + instr.getfBranch().getId());
            mipsAsm.add("j func_" + curFuncName + "_block_" + instr.gettBranch().getId());
        }
    }

    private static void parseCall(CallInstr instr) {
        final var retReg = instr.getToFunc().isVoidFunction() ? null : instr.getId();
        if (instr.getToFunc().getName().equals("getint")) {
            mipsAsm.add("addiu $v0, $0, 5");
            mipsAsm.add("syscall");
            curSPOffset -= 4;
            curVRegPos.put(retReg, curSPOffset);
            mipsAsm.add("sw $v0, " + curSPOffset + "($sp)");
            return;
        }
        if (instr.getToFunc().getName().equals("putch")) {
            mipsAsm.add("li $a0, " + ((IntConstant)instr.getArgs().get(0)).getValue());
            mipsAsm.add("addiu $v0, $0, 11");
            mipsAsm.add("syscall");
            return;
        }
        if (instr.getToFunc().getName().equals("putint")) {
            final var arg = instr.getArgs().get(0);
            if (arg instanceof IntConstant) mipsAsm.add("li $a0, " + ((IntConstant)arg).getValue());
            else mipsAsm.add("lw $a0, " + curVRegPos.get(arg.getId()) + "($sp)");
            mipsAsm.add("addiu $v0, $0, 1");
            mipsAsm.add("syscall");
            return;
        }
        final var argCount = instr.getArgs().size();
        mipsAsm.add("addu $fp, $0, $sp");
        mipsAsm.add("addiu $sp, $sp, " + (-(argCount * 4 - curSPOffset)));
        for (final var it = instr.getArgs().listIterator(); it.hasNext(); ) {
            final var arg = it.next();
            final var index = it.previousIndex();
            if (index < 4) { // use a0~a3
                if (arg instanceof IntConstant) mipsAsm.add("li $a" + index + ", " + ((IntConstant)arg).getValue());
                else mipsAsm.add("lw $a" + index + ", " + curVRegPos.get(arg.getId()) + "($fp)");
            } else { // use stack
                if (arg instanceof IntConstant) mipsAsm.add("li $t0, " + ((IntConstant)arg).getValue());
                else mipsAsm.add("lw $t0, " + curVRegPos.get(arg.getId()) + "($fp)");
                mipsAsm.add("sw $t0, " + (4 * index) + "($sp)");
            }
        }
        mipsAsm.add("jal func_" + instr.getToFunc().getName());
        mipsAsm.add("addiu $sp, $sp, " + (argCount * 4 - curSPOffset));
        if (retReg != null) {
            curSPOffset -= 4;
            curVRegPos.put(retReg, curSPOffset);
            mipsAsm.add("sw $v0, " + curSPOffset + "($sp)");
        }
    }

    private static void parseGetelementptr(GetelementptrInstr instr) {
        if (instr.getRefArray() instanceof GlobalVar) mipsAsm.add("la $t0, g_" + instr.getRefArray().getName() + "($0)");
        else if (curLocalVarPos.containsKey(instr.getRefArray().getId())) mipsAsm.add("la $t0, " + curLocalVarPos.get(instr.getRefArray().getId()) + "($sp)");
        else mipsAsm.add("lw $t0, " + curVRegPos.get(instr.getRefArray().getId()) + "($sp)");
        var curType = ((PointerType)instr.getRefArray().getValueType()).getDeref();
        var offset = 0;
        for (final var sub : instr.getSubs()) {
            final var curSize = sizeOfIRType(curType);
            if (sub instanceof IntConstant) {
                offset += curSize * ((IntConstant)sub).getValue();
            } else {
                mipsAsm.add("lw $t1, " + curVRegPos.get(sub.getId()) + "($sp)");
                mipsAsm.add("mul $t1, $t1, " + curSize);
                mipsAsm.add("addu $t0, $t0, $t1");
            }
            if (curType instanceof ArrayType) curType = ((ArrayType)curType).getElementType();
        }
        if (offset != 0) mipsAsm.add("addiu $t0, $t0, " + offset);
        curSPOffset -= 4;
        curVRegPos.put(instr.getId(), curSPOffset);
        mipsAsm.add("sw $t0, " + curSPOffset + "($sp)");
    }

    private static void parseIcmp(IcmpInstr instr) {
        String shift;
        switch (instr.getCond()) {
            case EQ: shift = "seq"; break;
            case NE: shift = "sne"; break;
            case SGE: shift = "sge"; break;
            case SGT: shift = "sgt"; break;
            case SLE: shift = "sle"; break;
            default /* SLT */: shift = "slt";
        }
        if (!(instr.getOp1() instanceof IntConstant) && !(instr.getOp2() instanceof IntConstant)) {
            mipsAsm.add("lw $t1, " + curVRegPos.get(instr.getOp1().getId()) + "($sp)");
            mipsAsm.add("lw $t2, " + curVRegPos.get(instr.getOp2().getId()) + "($sp)");
            mipsAsm.add(shift + " $t0, $t1, $t2");
        } else if (instr.getOp1() instanceof IntConstant) {
            switch (instr.getCond()) {
                case SGE: shift = "sle"; break;
                case SGT: shift = "slti"; break;
                case SLE: shift = "sge"; break;
                case SLT: shift = "sgt";
            }
            mipsAsm.add("lw $t1, " + curVRegPos.get(instr.getOp2().getId()) + "($sp)");
            final var imm = ((IntConstant)instr.getOp1()).getValue();
            if (instr.getCond() == IcmpInstr.Type.SLT && (imm < -32768 || imm > 32767)) {
                mipsAsm.add("li $t2, " + imm);
                mipsAsm.add("slt $t0, $t1, $t2");
            }
            else mipsAsm.add(shift + " $t0, $t1, " + imm);
        } else {
            if (instr.getCond() == IcmpInstr.Type.SLT) shift = "slti";
            mipsAsm.add("lw $t1, " + curVRegPos.get(instr.getOp1().getId()) + "($sp)");
            final var imm = ((IntConstant)instr.getOp2()).getValue();
            if (instr.getCond() == IcmpInstr.Type.SLT && (imm < -32768 || imm > 32767)) {
                mipsAsm.add("li $t2, " + imm);
                mipsAsm.add("slt $t0, $t1, $t2");
            }
            else mipsAsm.add(shift + " $t0, $t1, " + imm);
        }
        curSPOffset -= 4;
        curVRegPos.put(instr.getId(), curSPOffset);
        mipsAsm.add("sw $t0, " + curSPOffset + "($sp)");
    }

    private static void parseLoad(LoadInstr instr) {
        if (instr.getSrcPtr() instanceof GlobalVar) mipsAsm.add("lw $t0, g_" + instr.getSrcPtr().getName() + "($0)");
        else if (curLocalVarPos.containsKey(instr.getSrcPtr().getId())) mipsAsm.add("lw $t0, " + curLocalVarPos.get(instr.getSrcPtr().getId()) + "($sp)");
        else {
            mipsAsm.add("lw $t0, " + curVRegPos.get(instr.getSrcPtr().getId()) + "($sp)");
            mipsAsm.add("lw $t0, 0($t0)");
        }
        curSPOffset -= 4;
        curVRegPos.put(instr.getId(), curSPOffset);
        mipsAsm.add("sw $t0, " + curSPOffset + "($sp)");
    }

    private static void parseStore(StoreInstr instr) {
        if (instr.getStoreValue() instanceof IntConstant) mipsAsm.add("li $t0, " + ((IntConstant)instr.getStoreValue()).getValue());
        else {
            if (instr.getStoreValue().getId() < curFuncParamCount) { // is func param
                if (instr.getStoreValue().getId() < 4) {
                    curSPOffset -= 4;
                    curVRegPos.put(instr.getDstPtr().getId(), curSPOffset);
                    mipsAsm.add("sw $a" + instr.getStoreValue().getId() + ", " + curLocalVarPos.get(instr.getDstPtr().getId()) + "($sp)");
                    return;
                }
                mipsAsm.add("lw $t0, " + (4 * instr.getStoreValue().getId()) + "($sp)");
            }
            else mipsAsm.add("lw $t0, " + curVRegPos.get(instr.getStoreValue().getId()) + "($sp)");
        }
        if (instr.getDstPtr() instanceof GlobalVar) mipsAsm.add("sw $t0, g_" + instr.getDstPtr().getName() + "($0)");
        else if (curLocalVarPos.containsKey(instr.getDstPtr().getId())) mipsAsm.add("sw $t0, " + curLocalVarPos.get(instr.getDstPtr().getId()) + "($sp)");
        else {
            mipsAsm.add("lw $t1, " + curVRegPos.get(instr.getDstPtr().getId()) + "($sp)");
            mipsAsm.add("sw $t0, 0($t1)");
        }
    }

    private static void parseRet(RetInstr instr) {
        if (instr.getRetVal() != null) {
            if (instr.getRetVal() instanceof IntConstant) mipsAsm.add("li $v0, " + ((IntConstant)instr.getRetVal()).getValue());
            else mipsAsm.add("lw $v0, " + curVRegPos.get(instr.getRetVal().getId()) + "($sp)");
        }
        mipsAsm.add("lw $ra, -4($sp)");
        mipsAsm.add("jr $ra");
    }

    private static void parseZext(ZextInstr instr) {
        curVRegPos.put(instr.getId(), curVRegPos.get(instr.getToExt().getId()));
    }

    public static String emitOutput() {
        return String.join("\n", mipsAsm);
    }
}
