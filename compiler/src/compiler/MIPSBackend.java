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

import java.math.BigInteger;
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
    private static RegisterAllocator regAllocator;
    private static int specialCounter = 0;
    private static String memField;
    private static String prevReg;
    // ------ BEGIN MULT/DIV OPTMIZE VAR ------
    public static long multiplier;
    public static int shift;
    public static int log;
    // ------ END MULT/DIV OPTMIZE VAR ------

    public static void parseLLVM(Module module) {
        mipsAsm.add(".data");
        for (final var gv : module.getGlobalVars()) {
            parseGlobalVar(gv);
        }
        mipsAsm.add(".text");
        mipsAsm.add("jal func_main");
        mipsAsm.add("addiu $v0, $0, 10");
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
        regAllocator = new RegisterAllocator();
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
        final String op0, op1, op2;
        switch (instr.getInsType()) {
            case ADD:
                if (!(instr.getOp1() instanceof IntConstant) && !(instr.getOp2() instanceof IntConstant)) {
                    if (curVRegPos.containsKey(instr.getOp1().getId())) {
                        mipsAsm.add("lw $t8, " + curVRegPos.get(instr.getOp1().getId()) + "($sp)");
                        op1 = "t8";
                    } else op1 = Integer.toString(regAllocator.registerUse(instr.getOp1().getId()));
                    if (curVRegPos.containsKey(instr.getOp2().getId())) {
                        mipsAsm.add("lw $t9, " + curVRegPos.get(instr.getOp2().getId()) + "($sp)");
                        op2 = "t9";
                    } else op2 = Integer.toString(regAllocator.registerUse(instr.getOp2().getId()));
                    final var dst = regAllocator.registerAllocate(instr.getId());
                    op0 = (dst == -1) ? "v1" : Integer.toString(dst);
                    mipsAsm.add(String.format("addu $%s, $%s, $%s", op0, op1, op2));
                } else if (instr.getOp1() instanceof IntConstant) {
                    if (curVRegPos.containsKey(instr.getOp2().getId())) {
                        mipsAsm.add("lw $t9, " + curVRegPos.get(instr.getOp2().getId()) + "($sp)");
                        op2 = "t9";
                    } else op2 = Integer.toString(regAllocator.registerUse(instr.getOp2().getId()));
                    final var dst = regAllocator.registerAllocate(instr.getId());
                    op0 = (dst == -1) ? "v1" : Integer.toString(dst);
                    mipsAsm.add(String.format("addiu $%s, $%s, %d", op0, op2, ((IntConstant)instr.getOp1()).getValue()));
                } else {
                    if (curVRegPos.containsKey(instr.getOp1().getId())) {
                        mipsAsm.add("lw $t8, " + curVRegPos.get(instr.getOp1().getId()) + "($sp)");
                        op1 = "t8";
                    } else op1 = Integer.toString(regAllocator.registerUse(instr.getOp1().getId()));
                    final var dst = regAllocator.registerAllocate(instr.getId());
                    op0 = (dst == -1) ? "v1" : Integer.toString(dst);
                    mipsAsm.add(String.format("addiu $%s, $%s, %d", op0, op1, ((IntConstant)instr.getOp2()).getValue()));
                }
                break;
            case SUB:
                if (!(instr.getOp1() instanceof IntConstant) && !(instr.getOp2() instanceof IntConstant)) {
                    if (curVRegPos.containsKey(instr.getOp1().getId())) {
                        mipsAsm.add("lw $t8, " + curVRegPos.get(instr.getOp1().getId()) + "($sp)");
                        op1 = "t8";
                    } else op1 = Integer.toString(regAllocator.registerUse(instr.getOp1().getId()));
                    if (curVRegPos.containsKey(instr.getOp2().getId())) {
                        mipsAsm.add("lw $t9, " + curVRegPos.get(instr.getOp2().getId()) + "($sp)");
                        op2 = "t9";
                    } else op2 = Integer.toString(regAllocator.registerUse(instr.getOp2().getId()));
                    final var dst = regAllocator.registerAllocate(instr.getId());
                    op0 = (dst == -1) ? "v1" : Integer.toString(dst);
                    mipsAsm.add(String.format("subu $%s, $%s, $%s", op0, op1, op2));
                } else if (instr.getOp1() instanceof IntConstant) {
                    mipsAsm.add("li $t8, " + ((IntConstant)instr.getOp1()).getValue()); op1 = "t8";
                    if (curVRegPos.containsKey(instr.getOp2().getId())) {
                        mipsAsm.add("lw $t9, " + curVRegPos.get(instr.getOp2().getId()) + "($sp)");
                        op2 = "t9";
                    } else op2 = Integer.toString(regAllocator.registerUse(instr.getOp2().getId()));
                    final var dst = regAllocator.registerAllocate(instr.getId());
                    op0 = (dst == -1) ? "v1" : Integer.toString(dst);
                    mipsAsm.add(String.format("subu $%s, $%s, $%s", op0, op1, op2));
                } else {
                    final var v2 = ((IntConstant)instr.getOp2()).getValue();
                    if (curVRegPos.containsKey(instr.getOp1().getId())) {
                        mipsAsm.add("lw $t8, " + curVRegPos.get(instr.getOp1().getId()) + "($sp)");
                        op1 = "t8";
                    } else op1 = Integer.toString(regAllocator.registerUse(instr.getOp1().getId()));
                    final var dst = regAllocator.registerAllocate(instr.getId());
                    op0 = (dst == -1) ? "v1" : Integer.toString(dst);
                    if (v2 >= -32767 && v2 <= 32767) mipsAsm.add(String.format("addiu $%s, $%s, %d", op0, op1, -v2));
                    else mipsAsm.add(String.format("subiu $%s, $%s, %d", op0, op1, v2));
                }
                break;
            case MUL:
                if (!(instr.getOp1() instanceof IntConstant) && !(instr.getOp2() instanceof IntConstant)) {
                    if (curVRegPos.containsKey(instr.getOp1().getId())) {
                        mipsAsm.add("lw $t8, " + curVRegPos.get(instr.getOp1().getId()) + "($sp)");
                        op1 = "t8";
                    } else op1 = Integer.toString(regAllocator.registerUse(instr.getOp1().getId()));
                    if (curVRegPos.containsKey(instr.getOp2().getId())) {
                        mipsAsm.add("lw $t9, " + curVRegPos.get(instr.getOp2().getId()) + "($sp)");
                        op2 = "t9";
                    } else op2 = Integer.toString(regAllocator.registerUse(instr.getOp2().getId()));
                    final var dst = regAllocator.registerAllocate(instr.getId());
                    op0 = (dst == -1) ? "v1" : Integer.toString(dst);
                    mipsAsm.add(String.format("mul $%s, $%s, $%s", op0, op1, op2));
                } else if (instr.getOp1() instanceof IntConstant) {
                    if (curVRegPos.containsKey(instr.getOp2().getId())) {
                        mipsAsm.add("lw $t9, " + curVRegPos.get(instr.getOp2().getId()) + "($sp)");
                        op2 = "t9";
                    } else op2 = Integer.toString(regAllocator.registerUse(instr.getOp2().getId()));
                    final var dst = regAllocator.registerAllocate(instr.getId());
                    op0 = (dst == -1) ? "v1" : Integer.toString(dst);
                    if (isMultiplyOptimizable(((IntConstant)instr.getOp1()).getValue()))
                        mipsAsm.add(String.format("sll $%s, $%s, %d", op0, op2, factor2Shift(((IntConstant)instr.getOp1()).getValue())));
                    else mipsAsm.add(String.format("mul $%s, $%s, %d", op0, op2, ((IntConstant)instr.getOp1()).getValue()));
                } else {
                    if (curVRegPos.containsKey(instr.getOp1().getId())) {
                        mipsAsm.add("lw $t8, " + curVRegPos.get(instr.getOp1().getId()) + "($sp)");
                        op1 = "t8";
                    } else op1 = Integer.toString(regAllocator.registerUse(instr.getOp1().getId()));
                    final var dst = regAllocator.registerAllocate(instr.getId());
                    op0 = (dst == -1) ? "v1" : Integer.toString(dst);
                    if (isMultiplyOptimizable(((IntConstant)instr.getOp2()).getValue()))
                        mipsAsm.add(String.format("sll $%s, $%s, %d", op0, op1, factor2Shift(((IntConstant)instr.getOp2()).getValue())));
                    else mipsAsm.add(String.format("mul $%s, $%s, %d", op0, op1, ((IntConstant)instr.getOp2()).getValue()));
                }
                break;
            case SDIV:
                if (!(instr.getOp1() instanceof IntConstant) && !(instr.getOp2() instanceof IntConstant)) {
                    if (curVRegPos.containsKey(instr.getOp1().getId())) {
                        mipsAsm.add("lw $t8, " + curVRegPos.get(instr.getOp1().getId()) + "($sp)");
                        op1 = "t8";
                    } else op1 = Integer.toString(regAllocator.registerUse(instr.getOp1().getId()));
                    if (curVRegPos.containsKey(instr.getOp2().getId())) {
                        mipsAsm.add("lw $t9, " + curVRegPos.get(instr.getOp2().getId()) + "($sp)");
                        op2 = "t9";
                    } else op2 = Integer.toString(regAllocator.registerUse(instr.getOp2().getId()));
                    final var dst = regAllocator.registerAllocate(instr.getId());
                    op0 = (dst == -1) ? "v1" : Integer.toString(dst);
                    mipsAsm.add(String.format("div $%s, $%s, $%s", op0, op1, op2));
                } else if (instr.getOp1() instanceof IntConstant) {
                    mipsAsm.add("li $t8, " + ((IntConstant)instr.getOp1()).getValue()); op1 = "t8";
                    if (curVRegPos.containsKey(instr.getOp2().getId())) {
                        mipsAsm.add("lw $t9, " + curVRegPos.get(instr.getOp2().getId()) + "($sp)");
                        op2 = "t9";
                    } else op2 = Integer.toString(regAllocator.registerUse(instr.getOp2().getId()));
                    final var dst = regAllocator.registerAllocate(instr.getId());
                    op0 = (dst == -1) ? "v1" : Integer.toString(dst);
                    mipsAsm.add(String.format("div $%s, $%s, $%s", op0, op1, op2));
                } else {
                    if (curVRegPos.containsKey(instr.getOp1().getId())) {
                        mipsAsm.add("lw $t8, " + curVRegPos.get(instr.getOp1().getId()) + "($sp)");
                        op1 = "t8";
                    } else op1 = Integer.toString(regAllocator.registerUse(instr.getOp1().getId()));
                    final var dst = regAllocator.registerAllocate(instr.getId());
                    op0 = (dst == -1) ? "v1" : Integer.toString(dst);
                    optimizeDivide(instr, op1, op0);
                }
                break;
            case SREM:
                if (!(instr.getOp1() instanceof IntConstant) && !(instr.getOp2() instanceof IntConstant)) {
                    if (curVRegPos.containsKey(instr.getOp1().getId())) {
                        mipsAsm.add("lw $t8, " + curVRegPos.get(instr.getOp1().getId()) + "($sp)");
                        op1 = "t8";
                    } else op1 = Integer.toString(regAllocator.registerUse(instr.getOp1().getId()));
                    if (curVRegPos.containsKey(instr.getOp2().getId())) {
                        mipsAsm.add("lw $t9, " + curVRegPos.get(instr.getOp2().getId()) + "($sp)");
                        op2 = "t9";
                    } else op2 = Integer.toString(regAllocator.registerUse(instr.getOp2().getId()));
                    final var dst = regAllocator.registerAllocate(instr.getId());
                    op0 = (dst == -1) ? "v1" : Integer.toString(dst);
                    mipsAsm.add(String.format("rem $%s, $%s, $%s", op0, op1, op2));
                } else if (instr.getOp1() instanceof IntConstant) {
                    mipsAsm.add("li $t8, " + ((IntConstant)instr.getOp1()).getValue()); op1 = "t8";
                    if (curVRegPos.containsKey(instr.getOp2().getId())) {
                        mipsAsm.add("lw $t9, " + curVRegPos.get(instr.getOp2().getId()) + "($sp)");
                        op2 = "t9";
                    } else op2 = Integer.toString(regAllocator.registerUse(instr.getOp2().getId()));
                    final var dst = regAllocator.registerAllocate(instr.getId());
                    op0 = (dst == -1) ? "v1" : Integer.toString(dst);
                    mipsAsm.add(String.format("rem $%s, $%s, $%s", op0, op1, op2));
                } else {
                    final var dst = regAllocator.registerAllocate(instr.getId());
                    op0 = (dst == -1) ? "v1" : Integer.toString(dst);
                    if (curVRegPos.containsKey(instr.getOp1().getId())) {
                        mipsAsm.add("lw $t8, " + curVRegPos.get(instr.getOp1().getId()) + "($sp)");
                        op1 = "t8";
                    } else op1 = Integer.toString(regAllocator.registerUse(instr.getOp1().getId()));
                    if (isMultiplyOptimizable(((IntConstant)instr.getOp2()).getValue())) {
                        final var divisor = ((IntConstant) instr.getOp2()).getValue();
                        mipsAsm.add(String.format("andi $%s, $%s, %d", op0, op1, divisor - 1));
                        mipsAsm.add(String.format("bgez $%s, rem_opt_%d", op1, specialCounter));
                        mipsAsm.add(String.format("beq $%s, $0, rem_opt_%d", op0, specialCounter));
                        if (divisor >= -32767 && divisor <= 32767) mipsAsm.add(String.format("addiu $%s, $%s, %d", op0, op0, -divisor));
                        else mipsAsm.add(String.format("subiu $%s, $%s, %d", op0, op0, divisor));
                        mipsAsm.add(String.format("rem_opt_%d:", specialCounter++));
                    }
                    else mipsAsm.add(String.format("rem $%s, $%s, %d", op0, op1, ((IntConstant)instr.getOp2()).getValue()));
                }
                break;
            default: op0 = "";
        }
        if (op0.equals("v1")) {
            curSPOffset -= 4;
            curVRegPos.put(instr.getId(), curSPOffset);
            mipsAsm.add("sw $v1, " + curSPOffset + "($sp)");
        }
    }

    private static void optimizeDivide(ArithmeticInstr instr, String src, String dst) {
        boolean isDvsrNeg = ((IntConstant)instr.getOp2()).getValue() < 0;
        int divisor = Math.abs(((IntConstant)instr.getOp2()).getValue());
        chooseMultiplier(divisor);
        if (divisor == (1 << log)) {
            mipsAsm.add("sra $k0, $" + src + ", " + (log-1));
            mipsAsm.add("srl $k0, $k0, " + (32-log));
            mipsAsm.add("addu $k0, $k0, $" + src);
            mipsAsm.add("sra $" + dst + ", $k0, " + log);
        } else if (Long.numberOfLeadingZeros(multiplier) > 0 && multiplier < 0x80000000L) {
            int m = (int) multiplier;
            mipsAsm.add("li $k0, " + m);
            mipsAsm.add("mult $k0, $" + src);
            mipsAsm.add("mfhi $k0");
            mipsAsm.add("sra $k0, $k0, " + shift);
            mipsAsm.add("sra $k1, $" + src + ", 31");
            mipsAsm.add("subu $" + dst + ", $k0, $k1");
        } else {
            int m = (int) (multiplier - 0x100000000L);
            mipsAsm.add("li $k0, " + m);
            mipsAsm.add("mult $k0, $" + src);
            mipsAsm.add("mfhi $k0");
            mipsAsm.add("addu $k0, $k0, $" + src);
            mipsAsm.add("sra $k0, $k0, " + shift);
            mipsAsm.add("sra $k1, $" + src + ", 31");
            mipsAsm.add("subu $" + dst + ", $k0, $k1");
        }
        if (isDvsrNeg) mipsAsm.add("subu $" + dst + ", $0, $" + dst);
    }

    private static void chooseMultiplier(int divisor) {
        log = 32 - Integer.numberOfLeadingZeros(divisor - 1);
        shift = log;
        long low = BigInteger.valueOf(1).shiftLeft(32 + shift).divide(BigInteger.valueOf(divisor)).longValue();
        long high = BigInteger.valueOf(1).shiftLeft(32 + shift)
                .add(BigInteger.valueOf(1).shiftLeft(shift + 1)).divide(BigInteger.valueOf(divisor)).longValue();
        while ((low >> 1) < (high >> 1) && shift > 0) {
            low >>= 1;
            high >>= 1;
            shift--;
        }
        multiplier = high;
    }

    private static boolean isMultiplyOptimizable(int factor) {
        if ((factor & (factor - 1)) == 0)
            if (factor != 0x80000000) return true;
        return false;
    }

    private static int factor2Shift(int factor) {
        var shift = -1;
        while (factor != 0) {
            factor >>>= 1;
            shift++;
        }
        return shift;
    }

    private static void parseBr(BrInstr instr) {
        if (instr.getCond() == null) {
            mipsAsm.add("j func_" + curFuncName + "_block_" + instr.gettBranch().getId());
        } else {
            final String op;
            if (curVRegPos.containsKey(instr.getCond().getId())) {
                mipsAsm.add("lw $t8, " + curVRegPos.get(instr.getCond().getId()) + "($sp)");
                op = "t8";
            } else op = Integer.toString(regAllocator.registerUse(instr.getCond().getId()));
            mipsAsm.add(String.format("beq $%s, $0, func_%s_block_%d", op, curFuncName, instr.getfBranch().getId()));
            mipsAsm.add("j func_" + curFuncName + "_block_" + instr.gettBranch().getId());
        }
    }

    private static void parseCall(CallInstr instr) {
        final var retReg = instr.getToFunc().isVoidFunction() ? null : instr.getId();
        if (instr.getToFunc().getName().equals("getint")) {
            mipsAsm.add("addiu $v0, $0, 5");
            mipsAsm.add("syscall");
            final var dst = regAllocator.registerAllocate(instr.getId());
            if (dst == -1) {
                curSPOffset -= 4;
                curVRegPos.put(retReg, curSPOffset);
                mipsAsm.add("sw $v0, " + curSPOffset + "($sp)");
            } else mipsAsm.add(String.format("addu $%d, $0, $v0", dst));
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
            else if (curVRegPos.containsKey(arg.getId())) mipsAsm.add("lw $a0, " + curVRegPos.get(arg.getId()) + "($sp)");
            else mipsAsm.add(String.format("addu $a0, $0, $%d", regAllocator.registerUse(arg.getId())));
            mipsAsm.add("addiu $v0, $0, 1");
            mipsAsm.add("syscall");
            return;
        }
        final var argCount = instr.getArgs().size();
        final List<Integer> arl = new ArrayList<>(regAllocator.getActiveRegisters());
        for (var i = 0; i < arl.size(); i++) {
            curSPOffset -= 4;
            if (!instr.containsArgId(regAllocator.rreg2Vreg(arl.get(i)))) mipsAsm.add(String.format("sw $%d, %d($sp)", arl.get(i), curSPOffset));
        }
        mipsAsm.add("addu $fp, $0, $sp");
        mipsAsm.add("addiu $sp, $sp, " + (-(argCount * 4 - curSPOffset)));
        for (final var it = instr.getArgs().listIterator(); it.hasNext(); ) {
            final var arg = it.next();
            final var index = it.previousIndex();
            if (index < 4) { // use a0~a3
                if (arg instanceof IntConstant) mipsAsm.add("li $a" + index + ", " + ((IntConstant)arg).getValue());
                else if (curVRegPos.containsKey(arg.getId())) mipsAsm.add("lw $a" + index + ", " + curVRegPos.get(arg.getId()) + "($fp)");
                else mipsAsm.add(String.format("addu $a%d, $0, $%d", index, regAllocator.registerUse(arg.getId())));
            } else { // use stack
                final String op;
                if (arg instanceof IntConstant) {
                    mipsAsm.add("li $t8, " + ((IntConstant)arg).getValue());
                    op = "t8";
                } else if (curVRegPos.containsKey(arg.getId())) {
                    mipsAsm.add("lw $t8, " + curVRegPos.get(arg.getId()) + "($fp)");
                    op = "t8";
                } else op = Integer.toString(regAllocator.registerUse(arg.getId()));
                mipsAsm.add(String.format("sw $%s, %d($sp)", op, 4 * index));
            }
        }
        mipsAsm.add("jal func_" + instr.getToFunc().getName());
        mipsAsm.add("addiu $sp, $sp, " + (argCount * 4 - curSPOffset));
        for (var i = arl.size() - 1; i >= 0; i--) {
            if (regAllocator.getActiveRegisters().contains((arl.get(i)))) mipsAsm.add(String.format("lw $%d, %d($sp)", arl.get(i), curSPOffset));
            curSPOffset += 4;
        }
        if (retReg != null) {
            final var dst = regAllocator.registerAllocate(retReg);
            if (dst == -1) {
                curSPOffset -= 4;
                curVRegPos.put(retReg, curSPOffset);
                mipsAsm.add("sw $v0, " + curSPOffset + "($sp)");
            } else mipsAsm.add(String.format("addu $%d, $0, $v0", dst));
        }
    }

    private static void parseGetelementptr(GetelementptrInstr instr) {
        if (instr.getRefArray() instanceof GlobalVar) mipsAsm.add("la $t8, g_" + instr.getRefArray().getName() + "($0)");
        else if (curLocalVarPos.containsKey(instr.getRefArray().getId())) mipsAsm.add("la $t8, " + curLocalVarPos.get(instr.getRefArray().getId()) + "($sp)");
        else if (curVRegPos.containsKey(instr.getRefArray().getId())) mipsAsm.add("lw $t8, " + curVRegPos.get(instr.getRefArray().getId()) + "($sp)");
        else mipsAsm.add(String.format("addu $t8, $0, $%d", regAllocator.registerUse(instr.getRefArray().getId())));
        var curType = ((PointerType)instr.getRefArray().getValueType()).getDeref();
        var offset = 0;
        for (final var sub : instr.getSubs()) {
            final var curSize = sizeOfIRType(curType);
            if (sub instanceof IntConstant) {
                offset += curSize * ((IntConstant)sub).getValue();
            } else {
                final String opsub;
                if (curVRegPos.containsKey(sub.getId())) {
                    mipsAsm.add("lw $t9, " + curVRegPos.get(sub.getId()) + "($sp)");
                    opsub = "t9";
                } else opsub = Integer.toString(regAllocator.registerUse(sub.getId()));
                if (isMultiplyOptimizable(curSize))
                    mipsAsm.add(String.format("sll $%s, $%s, %d", opsub, opsub, factor2Shift(curSize)));
                else mipsAsm.add(String.format("mul $%s, $%s, %d", opsub, opsub, curSize));
                mipsAsm.add("addu $t8, $t8, $" + opsub);
            }
            if (curType instanceof ArrayType) curType = ((ArrayType)curType).getElementType();
        }
        if (offset != 0) mipsAsm.add("addiu $t8, $t8, " + offset);
        final var dst = regAllocator.registerAllocate(instr.getId());
        if (dst == -1) {
            curSPOffset -= 4;
            curVRegPos.put(instr.getId(), curSPOffset);
            mipsAsm.add("sw $t8, " + curSPOffset + "($sp)");
        } else mipsAsm.add(String.format("addu $%d, $0, $t8", dst));
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
        final String op0, op1, op2;
        if (!(instr.getOp1() instanceof IntConstant) && !(instr.getOp2() instanceof IntConstant)) {
            if (curVRegPos.containsKey(instr.getOp1().getId())) {
                mipsAsm.add("lw $t8, " + curVRegPos.get(instr.getOp1().getId()) + "($sp)");
                op1 = "t8";
            } else op1 = Integer.toString(regAllocator.registerUse(instr.getOp1().getId()));
            if (curVRegPos.containsKey(instr.getOp2().getId())) {
                mipsAsm.add("lw $t9, " + curVRegPos.get(instr.getOp2().getId()) + "($sp)");
                op2 = "t9";
            } else op2 = Integer.toString(regAllocator.registerUse(instr.getOp2().getId()));
            final var dst = regAllocator.registerAllocate(instr.getId());
            op0 = (dst == -1) ? "v1" : Integer.toString(dst);
            mipsAsm.add(String.format("%s $%s, $%s, $%s", shift, op0, op1, op2));
        } else if (instr.getOp1() instanceof IntConstant) {
            switch (instr.getCond()) {
                case SGE: shift = "sle"; break;
                case SGT: shift = "slti"; break;
                case SLE: shift = "sge"; break;
                case SLT: shift = "sgt";
            }
            if (curVRegPos.containsKey(instr.getOp2().getId())) {
                mipsAsm.add("lw $t9, " + curVRegPos.get(instr.getOp2().getId()) + "($sp)");
                op2 = "t9";
            } else op2 = Integer.toString(regAllocator.registerUse(instr.getOp2().getId()));
            final var dst = regAllocator.registerAllocate(instr.getId());
            op0 = (dst == -1) ? "v1" : Integer.toString(dst);
            final var imm = ((IntConstant)instr.getOp1()).getValue();
            if (instr.getCond() == IcmpInstr.Type.SLT && (imm < -32768 || imm > 32767)) {
                mipsAsm.add("li $t8, " + imm);
                mipsAsm.add(String.format("slt $%s, $%s, $t8", op0, op2));
            }
            else mipsAsm.add(String.format("%s $%s, $%s, %d", shift, op0, op2, imm));
        } else {
            if (instr.getCond() == IcmpInstr.Type.SLT) shift = "slti";
            if (curVRegPos.containsKey(instr.getOp1().getId())) {
                mipsAsm.add("lw $t8, " + curVRegPos.get(instr.getOp1().getId()) + "($sp)");
                op1 = "t8";
            } else op1 = Integer.toString(regAllocator.registerUse(instr.getOp1().getId()));
            final var dst = regAllocator.registerAllocate(instr.getId());
            op0 = (dst == -1) ? "v1" : Integer.toString(dst);
            final var imm = ((IntConstant)instr.getOp2()).getValue();
            if (instr.getCond() == IcmpInstr.Type.SLT && (imm < -32768 || imm > 32767)) {
                mipsAsm.add("li $t9, " + imm);
                mipsAsm.add(String.format("slt $%s, $%s, $t9", op0, op1));
            }
            else mipsAsm.add(String.format("%s $%s, $%s, %d", shift, op0, op1, imm));
        }
        if (op0.equals("v1")) {
            curSPOffset -= 4;
            curVRegPos.put(instr.getId(), curSPOffset);
            mipsAsm.add("sw $v1, " + curSPOffset + "($sp)");
        }
    }

    private static void parseLoad(LoadInstr instr) {
        final var dst = regAllocator.registerAllocate(instr.getId());
        final var op = (dst == -1) ? "v1" : Integer.toString(dst);
        if (instr.getSrcPtr() instanceof GlobalVar) mipsAsm.add(String.format("lw $%s, g_%s($0)", op, instr.getSrcPtr().getName()));
        else if (curLocalVarPos.containsKey(instr.getSrcPtr().getId())) mipsAsm.add(String.format("lw $%s, %d($sp)", op, curLocalVarPos.get(instr.getSrcPtr().getId())));
        else if (curVRegPos.containsKey(instr.getSrcPtr().getId())) {
            mipsAsm.add(String.format("lw $%s, %d($sp)", op, curVRegPos.get(instr.getSrcPtr().getId())));
            mipsAsm.add(String.format("lw $%s, 0($%s)", op, op));
        } else mipsAsm.add(String.format("lw $%s, 0($%d)", op, regAllocator.registerUse(instr.getSrcPtr().getId())));
        if (op.equals("v1")) {
            curSPOffset -= 4;
            curVRegPos.put(instr.getId(), curSPOffset);
            mipsAsm.add("sw $v1, " + curSPOffset + "($sp)");
        }
    }

    private static void parseStore(StoreInstr instr) {
        final String ops;
        if (instr.getStoreValue() instanceof IntConstant) {
            mipsAsm.add("li $t8, " + ((IntConstant)instr.getStoreValue()).getValue());
            ops = "t8";
        } else {
            if (instr.getStoreValue().getId() < curFuncParamCount) { // is func param
                if (instr.getStoreValue().getId() < 4) {
                    curSPOffset -= 4;
                    curVRegPos.put(instr.getDstPtr().getId(), curSPOffset);
                    mipsAsm.add("sw $a" + instr.getStoreValue().getId() + ", " + curLocalVarPos.get(instr.getDstPtr().getId()) + "($sp)");
                    return;
                }
                mipsAsm.add("lw $t8, " + (4 * instr.getStoreValue().getId()) + "($sp)");
                ops = "t8";
            }
            else if (curVRegPos.containsKey(instr.getStoreValue().getId())) {
                mipsAsm.add("lw $t8, " + curVRegPos.get(instr.getStoreValue().getId()) + "($sp)");
                ops = "t8";
            } else ops = Integer.toString(regAllocator.registerUse(instr.getStoreValue().getId()));
        }
        if (instr.getDstPtr() instanceof GlobalVar) mipsAsm.add(String.format("sw $%s, g_%s($0)", ops, instr.getDstPtr().getName()));
        else if (curLocalVarPos.containsKey(instr.getDstPtr().getId())) mipsAsm.add(String.format("sw $%s, %d($sp)", ops, curLocalVarPos.get(instr.getDstPtr().getId())));
        else if (curVRegPos.containsKey(instr.getDstPtr().getId())) {
            mipsAsm.add(String.format("lw $t9, %d($sp)", curVRegPos.get(instr.getDstPtr().getId())));
            mipsAsm.add(String.format("sw $%s, 0($t9)", ops));
        } else mipsAsm.add(String.format("sw $%s, 0($%d)", ops, regAllocator.registerUse(instr.getDstPtr().getId())));
    }

    private static void parseRet(RetInstr instr) {
        if (instr.getRetVal() != null) {
            if (instr.getRetVal() instanceof IntConstant) mipsAsm.add("li $v0, " + ((IntConstant)instr.getRetVal()).getValue());
            else if (curVRegPos.containsKey(instr.getRetVal().getId())) mipsAsm.add("lw $v0, " + curVRegPos.get(instr.getRetVal().getId()) + "($sp)");
            else mipsAsm.add("addu $v0, $0, $" + regAllocator.registerUse(instr.getRetVal().getId()));
        }
        mipsAsm.add("lw $ra, -4($sp)");
        mipsAsm.add("jr $ra");
    }

    private static void parseZext(ZextInstr instr) {
        if (curVRegPos.containsKey(instr.getToExt().getId()))
            curVRegPos.put(instr.getId(), curVRegPos.get(instr.getToExt().getId()));
        else regAllocator.moveRegister(instr.getToExt().getId(), instr.getId());
    }

    public static String emitOutput() {
        final var sb = new StringBuilder();
        for (var i = 0; i < mipsAsm.size(); i++) {
            final var line = mipsAsm.get(i);
            if (line.startsWith("j ") && (i+1)<mipsAsm.size()) {
                memField = null; prevReg = null;
                if (mipsAsm.get(i+1).endsWith(":") &&
                        line.substring(2).equals(mipsAsm.get(i+1).substring(0, mipsAsm.get(i+1).length()-1)))
                    continue;
            } else if (line.startsWith("lw ")) {
                final var oper = line.split(" ");
                if (oper[2].equals(memField)) {
                    final var reg = oper[1].substring(0, oper[1].length()-1);
                    if (!reg.equals(prevReg)) sb.append(String.format("addu %s, $0, %s\n", reg, prevReg));
                    memField = null; prevReg = null;
                    continue;
                }
                memField = null; prevReg = null;
            } else if (line.startsWith("sw ")) {
                final var oper = line.split(" ");
                memField = oper[2];
                final var tmp = oper[1];
                prevReg = tmp.substring(0, tmp.length()-1);
            } else {
                memField = null; prevReg = null;
            }
            sb.append(line).append('\n');
        }
        return sb.toString();
    }
}
