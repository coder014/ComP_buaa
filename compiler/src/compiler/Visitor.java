package compiler;

import ir.*;
import ir.Module;
import ir.instr.*;
import ir.type.*;
import nonterm.*;
import symbol.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class Visitor {
    private static int loopCount = 0;
    private static boolean isVoidFunction = false;
    private static Function curIRFunction;
    private static BasicBlock curIRBasicBlock;
    /*
     * ifBBS stack overlay:
     * -----------------------------------------------
     * | NormalStmt | (ElseStmt) | IfStmt | ......
     * -----------------------------------------------
     *
     * forBBS stack overlay:
     * ------------------------------------------------------
     * | Cond | (ForLoopStmt) | NormalStmt | Stmt | ......
     * ------------------------------------------------------
     */
    private static final LinkedList<BasicBlock> ifBBS = new LinkedList<>(); // BBS is for BasicBlock Stack
    private static final LinkedList<BasicBlock> forBBS = new LinkedList<>(); // BBS is for BasicBlock Stack
    private static BasicBlock ifTrueBasicBlock, ifFalseBasicBlock;
    private static Value lValueIRRes;
    private static final Function exGetint = new Function("getint", new FunctionType(IntType.INT, List.of()));
    private static final Function exPutint = new Function("putint", new FunctionType(VoidType.INSTANCE, List.of(IntType.INT)));
    private static final Function exPutch = new Function("putch", new FunctionType(VoidType.INSTANCE, List.of(IntType.INT)));

    public static void visitCompUnit(CompUnit node) {
        for (var decl : node.getDecls()) visitDecl(decl);
        for (var funcDef : node.getFuncDefs()) visitFuncDef(funcDef);
        visitFuncDef(node.getMainFunc());
    }

    private static void visitDecl(Decl node) {
        final var decl = node.getVarDecl();
        if (decl != null) visitDecl(decl);
        else visitDecl(node.getConstDecl());
    }

    private static void visitFuncDef(FuncDef node) {
        final var fIdent = node.getIdent();
        final var fType = node.getType().getType();
        final var block = node.getBlock();
        final var pTypeList = new ArrayList<SymbolType>();
        final var irPTypeList = new ArrayList<ValueType>();
        final var funcSym = new Symbol(fIdent.getValue(), new SymbolType(fType, pTypeList));
        if (!SymbolTable.getCurrent().appendSymbol(funcSym)) {
            CompError.appendError(fIdent.getLineNum(), 'b', "Duplicated definition for func " + fIdent.getValue());
        }
        Utils.resetCounter();
        SymbolTable.enterNewScope();
        if (node.getParams() != null) {
            final var params = node.getParams().getParamList();
            for (var param : params) {
                final Symbol sym;
                final SymbolType pType;
                final ValueType irPType;
                // assuming param's dimension Exp is correct
                if (param.getExps().isEmpty()) { // variable
                    pType = new SymbolType(false);
                    irPType = IntType.INT;
                } else if (param.getExps().size() == 1) { // 1-array pointer
                    pType = new SymbolType(false, List.of(0));
                    irPType = new PointerType(IntType.INT);
                } else { // 2-array pointer
                    final var dim2 = visitExp(param.getExps().get(1)).getValue();
                    pType = new SymbolType(false, List.of(0, dim2));
                    irPType = new PointerType(new ArrayType(dim2, IntType.INT));
                }
                sym = new Symbol(param.getIdent().getValue(), pType);
                if (!SymbolTable.getCurrent().appendSymbol(sym)) {
                    CompError.appendError(param.getIdent().getLineNum(), 'b', "Duplicated definition for param " + param.getIdent().getValue());
                } else {
                    pTypeList.add(pType);
                    irPTypeList.add(irPType);
                }
            }
        }
        isVoidFunction = fType == Token.Type.VOIDTK;
        curIRFunction = new Function(fIdent.getValue(),
                new FunctionType(isVoidFunction ? VoidType.INSTANCE : IntType.INT, irPTypeList));
        funcSym.setIrValue(curIRFunction);
        curIRBasicBlock = new BasicBlock(Utils.getIncCounter());
        curIRFunction.appendBasicBlock(curIRBasicBlock);
        if (node.getParams() != null) prepareFuncStack(node.getParams().getParamList());
        visitBlock(block, false);
        final var hasEndingRet = funcBlockHasEndingReturn(node.getBlock());
        if (!isVoidFunction && !hasEndingRet)
            CompError.appendError(block.getEndingLineNum(), 'g', "Missing ending return for function with ret-value");
        if (isVoidFunction && !hasEndingRet) // missing ending return in void func, manually adding one
            curIRBasicBlock.appendInstruction(new RetInstr());
        SymbolTable.exitCurrentScope();
        Module.INSTANCE.appendFunction(curIRFunction);
    }

    private static void prepareFuncStack(List<FuncFParam> params) {
        final var irParams = curIRFunction.getParams();
        final var instrList = new ArrayList<Value>();
        for (int i = 0; i < params.size(); i++) { // allocate formal parameters' virtual regs
            final var instr = new AllocaInstr(Utils.getIncCounter(), irParams.get(i).getValueType());
            curIRBasicBlock.appendInstruction(instr);
            instrList.add(instr);
            SymbolTable.getSymbolByName(params.get(i).getIdent().getValue()).setIrValue(instr);
        }
        for (int i = 0; i < irParams.size(); i++) { // store real parameters value to allocated
            final var instr = new StoreInstr(irParams.get(i), instrList.get(i));
            curIRBasicBlock.appendInstruction(instr);
        }
    }

    private static void visitFuncDef(MainFuncDef node) {
        Utils.resetCounter();
        final var sym = new Symbol("main", new SymbolType(Token.Type.INTTK, List.of()));
        SymbolTable.getCurrent().appendSymbol(sym);
        curIRFunction = new Function("main", new FunctionType(IntType.INT, List.of()));
        SymbolTable.enterNewScope();
        isVoidFunction = false;
        curIRBasicBlock = new BasicBlock(Utils.getIncCounter());
        curIRFunction.appendBasicBlock(curIRBasicBlock);
        visitBlock(node.getBlock(), false);
        if (!funcBlockHasEndingReturn(node.getBlock()))
            CompError.appendError(node.getBlock().getEndingLineNum(), 'g', "Missing ending return for main function");
        SymbolTable.exitCurrentScope();
        Module.INSTANCE.appendFunction(curIRFunction);
    }

    private static boolean funcBlockHasEndingReturn(Block node) {
        if (node.getItems().isEmpty()) return false;
        final var stmt = node.getItems().get(node.getItems().size() - 1).getStmt();
        if (stmt == null) return false;
        return stmt.getLexType() == 7;
    }

    private static void visitBlock(Block node, boolean newScope) {
        if (newScope) SymbolTable.enterNewScope();
        for (var bItem : node.getItems()) {
            if (bItem.getStmt() != null) visitStmt(bItem.getStmt());
            else visitDecl(bItem.getDecl());
        }
        if (newScope) SymbolTable.exitCurrentScope();
    }

    private static void visitDecl(ConstDecl node) {
        // assume btype is int
        for (var def : node.getDefs()) visitDef(def);
    }

    private static void visitDef(ConstDef node) {
        // assume btype is int
        // ! keep updated with VarDef !
        final Symbol sym;
        ExpInfo info;
        ConstInitVal initVal = node.getVal();
        if (node.getExps().isEmpty()) { // constant
            sym = new Symbol(node.getIdent().getValue(), new SymbolType(true));
            info = visitExp(initVal.getExp());
            sym.setIntValue(info.getValue());
        } else if (node.getExps().size() == 1) { // 1-array
            info = visitExp(node.getExps().get(0));
            sym = new Symbol(node.getIdent().getValue(), new SymbolType(true, List.of(info.getValue())));
            final var irType = new ArrayType(info.getValue(), IntType.INT);
            if (SymbolTable.getCurrent().isRoot()) {
                final var list = new ArrayList<Integer>();
                for (final var val : initVal.getVals())
                    list.add(visitExp(val.getExp()).getValue());
                final var gv = new GlobalVar(sym.getName(), irType, list);
                sym.setIrValue(gv);
                Module.INSTANCE.appendGlobalVar(gv);
            } else {
                final var alloc = new AllocaInstr(Utils.getIncCounter(), irType);
                sym.setIrValue(alloc);
                curIRBasicBlock.appendInstruction(alloc);
                for (int i = 0; i < initVal.getVals().size(); i++) {
                    info = visitExp(initVal.getVals().get(i).getExp());
                    final var res = new GetelementptrInstr(Utils.getIncCounter(), new PointerType(IntType.INT), alloc,
                            new IntConstant(0), new IntConstant(i));
                    curIRBasicBlock.appendInstruction(res);
                    curIRBasicBlock.appendInstruction(new StoreInstr(info.getResIR(), res));
                }
            }
        } else { // 2-array
            info = visitExp(node.getExps().get(0));
            final var info2 = visitExp(node.getExps().get(1));
            sym = new Symbol(node.getIdent().getValue(), new SymbolType(true, List.of(info.getValue(), info2.getValue())));
            final var irType = new ArrayType(info.getValue(), new ArrayType(info2.getValue(), IntType.INT));
            if (SymbolTable.getCurrent().isRoot()) {
                final var list = new ArrayList<List<Integer>>();
                for (final var val1 : initVal.getVals()) {
                    final var inner = new ArrayList<Integer>();
                    for (final var val2 : val1.getVals())
                        inner.add(visitExp(val2.getExp()).getValue());
                    list.add(inner);
                }
                final var gv = new GlobalVar(sym.getName(), irType, list, false);
                sym.setIrValue(gv);
                Module.INSTANCE.appendGlobalVar(gv);
            } else {
                final var alloc = new AllocaInstr(Utils.getIncCounter(), irType);
                sym.setIrValue(alloc);
                curIRBasicBlock.appendInstruction(alloc);
                for (int i = 0; i < initVal.getVals().size(); i++) {
                    final var tVal = initVal.getVals().get(i);
                    for (int j = 0; j < tVal.getVals().size(); j++) {
                        info = visitExp(tVal.getVals().get(j).getExp());
                        final var res = new GetelementptrInstr(Utils.getIncCounter(), new PointerType(IntType.INT), alloc,
                                new IntConstant(0), new IntConstant(i), new IntConstant(j));
                        curIRBasicBlock.appendInstruction(res);
                        curIRBasicBlock.appendInstruction(new StoreInstr(info.getResIR(), res));
                    }
                }
            }
        }
        if (!SymbolTable.getCurrent().appendSymbol(sym)) {
            CompError.appendError(node.getIdent().getLineNum(), 'b', "Duplicated definition for ident " + node.getIdent().getValue());
        }
    }

    private static void visitDecl(VarDecl node) {
        // assume btype is int
        for (var def : node.getDefs()) visitDef(def);
    }

    private static void visitDef(VarDef node) {
        // assume btype is int
        // ! keep updated with ConstDef !
        final Symbol sym;
        ExpInfo info = null;
        InitVal initVal = node.getVal();
        if (node.getExps().isEmpty()) { // variable
            sym = new Symbol(node.getIdent().getValue(), new SymbolType(false));
            if (SymbolTable.getCurrent().isRoot()) {
                if (initVal != null) info = visitExp(initVal.getExp());
                final var gv = new GlobalVar(sym.getName(), IntType.INT, info != null ? info.getValue() : 0);
                sym.setIrValue(gv);
                Module.INSTANCE.appendGlobalVar(gv);
            } else {
                final var alloc = new AllocaInstr(Utils.getIncCounter(), IntType.INT);
                sym.setIrValue(alloc);
                curIRBasicBlock.appendInstruction(alloc);
                if (initVal != null) {
                    info = visitExp(initVal.getExp());
                    curIRBasicBlock.appendInstruction(new StoreInstr(info.getResIR(), alloc));
                }
            }
        } else if (node.getExps().size() == 1) { // 1-array
            info = visitExp(node.getExps().get(0));
            sym = new Symbol(node.getIdent().getValue(), new SymbolType(false, List.of(info.getValue())));
            final var irType = new ArrayType(info.getValue(), IntType.INT);
            if (SymbolTable.getCurrent().isRoot()) {
                final var list = new ArrayList<Integer>();
                if (initVal != null)
                    for (final var val : initVal.getVals())
                        list.add(visitExp(val.getExp()).getValue());
                final var gv = new GlobalVar(sym.getName(), irType, list);
                sym.setIrValue(gv);
                Module.INSTANCE.appendGlobalVar(gv);
            } else {
                final var alloc = new AllocaInstr(Utils.getIncCounter(), irType);
                sym.setIrValue(alloc);
                curIRBasicBlock.appendInstruction(alloc);
                if (initVal != null) for (int i = 0; i < initVal.getVals().size(); i++) {
                    info = visitExp(initVal.getVals().get(i).getExp());
                    final var res = new GetelementptrInstr(Utils.getIncCounter(), new PointerType(IntType.INT), alloc,
                            new IntConstant(0), new IntConstant(i));
                    curIRBasicBlock.appendInstruction(res);
                    curIRBasicBlock.appendInstruction(new StoreInstr(info.getResIR(), res));
                }
            }
        } else { // 2-array
            info = visitExp(node.getExps().get(0));
            final var info2 = visitExp(node.getExps().get(1));
            sym = new Symbol(node.getIdent().getValue(), new SymbolType(false, List.of(info.getValue(), info2.getValue())));
            final var irType = new ArrayType(info.getValue(), new ArrayType(info2.getValue(), IntType.INT));
            if (SymbolTable.getCurrent().isRoot()) {
                final var list = new ArrayList<List<Integer>>();
                if (initVal != null)
                    for (final var val1 : initVal.getVals()) {
                        final var inner = new ArrayList<Integer>();
                        for (final var val2 : val1.getVals())
                            inner.add(visitExp(val2.getExp()).getValue());
                        list.add(inner);
                    }
                final var gv = new GlobalVar(sym.getName(), irType, list, false);
                sym.setIrValue(gv);
                Module.INSTANCE.appendGlobalVar(gv);
            } else {
                final var alloc = new AllocaInstr(Utils.getIncCounter(), irType);
                sym.setIrValue(alloc);
                curIRBasicBlock.appendInstruction(alloc);
                if (initVal != null) for (int i = 0; i < initVal.getVals().size(); i++) {
                    final var tVal = initVal.getVals().get(i);
                    for (int j = 0; j < tVal.getVals().size(); j++) {
                        info = visitExp(tVal.getVals().get(j).getExp());
                        final var res = new GetelementptrInstr(Utils.getIncCounter(), new PointerType(IntType.INT), alloc,
                                new IntConstant(0), new IntConstant(i), new IntConstant(j));
                        curIRBasicBlock.appendInstruction(res);
                        curIRBasicBlock.appendInstruction(new StoreInstr(info.getResIR(), res));
                    }
                }
            }
        }
        if (!SymbolTable.getCurrent().appendSymbol(sym)) {
            CompError.appendError(node.getIdent().getLineNum(), 'b', "Duplicated definition for ident " + node.getIdent().getValue());
        }
    }

    private static ExpInfo visitExp(ConstExp node) {
        return visitExp(node.getExp());
    }

    private static ExpInfo visitExp(Exp node) {
        return visitExp(node.getExp());
    }

    private static ExpInfo visitExp(AddExp node) {
        final ExpInfo info1, info2;
        if (node.getOp() == null) { // single MulExp
            return visitExp(node.getRExp());
        } else { // AddExp + MulExp
            info1 = visitExp(node.getLExp());
            info2 = visitExp(node.getRExp()); // assuming dimension is equal
        }
        if (info1.isConst() && info2.isConst()) { // constant optimization
            if (node.getOp().getType() == Token.Type.MINU) return new ExpInfo(false, info1.getValue() - info2.getValue(), 0);
            else return new ExpInfo(false, info1.getValue() + info2.getValue(), 0);
        } else {
            final ArithmeticInstr instr = new ArithmeticInstr(
                    node.getOp().getType() == Token.Type.MINU ? ArithmeticInstr.Type.SUB : ArithmeticInstr.Type.ADD,
                    Utils.getIncCounter(), info1.getResIR(), info2.getResIR());
            curIRBasicBlock.appendInstruction(instr);
            return new ExpInfo(false, 0, instr);
        }
    }

    private static ExpInfo visitExp(MulExp node) {
        final ExpInfo info1, info2;
        if (node.getOp() == null) { // single UnaryExp
            return visitExp(node.getRExp());
        } else { // MulExp * UnaryExp
            info1 = visitExp(node.getLExp());
            info2 = visitExp(node.getRExp()); // assuming dimension is equal
        }
        if (info1.isConst() && info2.isConst()) { // constant optimization
            if (node.getOp().getType() == Token.Type.MULT) return new ExpInfo(false, info1.getValue() * info2.getValue(), 0);
            else if (node.getOp().getType() == Token.Type.DIV) return new ExpInfo(false, info1.getValue() / info2.getValue(), 0);
            else return new ExpInfo(false, info1.getValue() % info2.getValue(), 0);
        } else {
            final ArithmeticInstr instr = new ArithmeticInstr(
                    node.getOp().getType() == Token.Type.MULT ? ArithmeticInstr.Type.MUL :
                    node.getOp().getType() == Token.Type.DIV ? ArithmeticInstr.Type.SDIV : ArithmeticInstr.Type.SREM,
                    Utils.getIncCounter(), info1.getResIR(), info2.getResIR());
            curIRBasicBlock.appendInstruction(instr);
            return new ExpInfo(false, 0, instr);
        }
    }

    private static ExpInfo visitExp(UnaryExp node) {
        ExpInfo info;
        if (node.getPExp() != null) { // PrimaryExp
            info = visitExp(node.getPExp());
        } else if (node.getIdent() != null) { // func(call params)
            final var sym = SymbolTable.getSymbolByName(node.getIdent().getValue());
            final var rParamList = new ArrayList<Value>();
            if (sym == null) {
                CompError.appendError(node.getIdent().getLineNum(), 'c', "undefined symbol " + node.getIdent().getValue());
                return new ExpInfo(false, null, -1);
            }
            if (sym.getType().getCategory() != SymbolType.Category.FUNC) return new ExpInfo(false, null, -1); // shall be function call
            if (node.getRParams() != null) { // has params
                if (sym.getType().getParams().size() != node.getRParams().getExps().size())
                    CompError.appendError(node.getIdent().getLineNum(), 'd', "unmatched params count for function call " + node.getIdent().getValue());
                else {
                    Iterator<SymbolType> itFParamType = sym.getType().getParams().iterator();
                    for (var exp : node.getRParams().getExps()) {
                        final var fParamType = itFParamType.next();
                        final var rParamInfo = visitExp(exp);
                        if (rParamInfo.getDimension() != fParamType.getDimension())
                            CompError.appendError(node.getIdent().getLineNum(), 'e',
                                    String.format("unmatched params type for function %s, FParam is %d-dim and RParam is %d-dim", node.getIdent().getValue(), fParamType.getDimension(), rParamInfo.getDimension()));
                        else rParamList.add(rParamInfo.getResIR());
                    }
                }
            } else { // 0 param
                if (!sym.getType().getParams().isEmpty())
                    CompError.appendError(node.getIdent().getLineNum(), 'd', "unmatched params count for function call " + node.getIdent().getValue());
            }
            final var call = sym.getType().getRetType() == Token.Type.VOIDTK ?
                    new CallInstr((Function) sym.getIrValue(), rParamList) :
                    new CallInstr(Utils.getIncCounter(), (Function) sym.getIrValue(), rParamList);
            curIRBasicBlock.appendInstruction(call);
            info = new ExpInfo(false, sym.getType().getRetType() == Token.Type.VOIDTK ? -1 : 0, call);
        } else { // -UnaryExp
            info = visitExp(node.getUExp());
            if (info.isConst()) { // constant optimization
                if (node.getUOp().getType() == Token.Type.MINU) info = new ExpInfo(false, -info.getValue(), 0);
                else if(node.getUOp().getType() == Token.Type.NOT) info = new ExpInfo(false, info.getValue() == 0 ? 1 : 0, 0);
            } else if (node.getUOp().getType() == Token.Type.MINU) {
                final Instruction instr = new ArithmeticInstr(ArithmeticInstr.Type.SUB,
                        Utils.getIncCounter(), new IntConstant(0), info.getResIR());
                curIRBasicBlock.appendInstruction(instr);
                info = new ExpInfo(false, 0, instr);
            } else if (node.getUOp().getType() == Token.Type.NOT) {
                Instruction instr = new IcmpInstr(IcmpInstr.Type.EQ, Utils.getIncCounter(), info.getResIR(), new IntConstant(0));
                curIRBasicBlock.appendInstruction(instr);
                instr = new ZextInstr(IntType.INT, Utils.getIncCounter(), instr);
                curIRBasicBlock.appendInstruction(instr);
                info = new ExpInfo(false, 0, instr);
            }
        }
        return info;
    }

    private static ExpInfo visitExp(PrimaryExp node) {
        final ExpInfo info;
        if (node.getNumber() != null) { // Number
            info = new ExpInfo(false, node.getNumber().getIntValue(), 0);
        } else if (node.getLVal() != null) { // LVal
            final var sym = visitLVal(node.getLVal());
            if (sym == null) info = new ExpInfo(false, null, -1); // error: missing symbol
            else if (lValueIRRes instanceof IntConstant) { // const variable
                info = new ExpInfo(false, ((IntConstant)lValueIRRes).getValue(), 0);
            } else {
                if (sym.getType().getDimension() - node.getLVal().getExps().size() == 0) { // int
                    final var instr = new LoadInstr(Utils.getIncCounter(), lValueIRRes);
                    curIRBasicBlock.appendInstruction(instr);
                    info = new ExpInfo(false, 0, instr);
                } else info = new ExpInfo(false, sym.getType().getDimension() - node.getLVal().getExps().size(), lValueIRRes);
            }
        } else { // (exp)
            info = visitExp(node.getExp());
        }
        return info;
    }

    private static Symbol visitLVal(LVal node) {
        final var sym = SymbolTable.getSymbolByName(node.getIdent().getValue());
        if (sym == null) {
            CompError.appendError(node.getIdent().getLineNum(), 'c', "Undefined symbol " + node.getIdent().getValue());
            lValueIRRes = null;
            return null;
        }
        if (sym.getType().getDimension() == 0) {
            if (sym.getType().isConst()) { // basic const
                lValueIRRes = new IntConstant(sym.getIntValue());
            } else {
                lValueIRRes = sym.getIrValue();
            }
        } else {
            Instruction res;
            final Value arr;
            if (sym.getType().isParamArray()) {
                res = new LoadInstr(Utils.getIncCounter(), sym.getIrValue());
                curIRBasicBlock.appendInstruction(res);
                arr = res;
            } else arr = sym.getIrValue();
            final LinkedList<Value> subs = new LinkedList<>();
            for (var exp : node.getExps()) subs.add(visitExp(exp).getResIR());
            if (!sym.getType().isParamArray()) subs.addFirst(new IntConstant(0));
            // array as RValue, downcasting to lower-dimed pointer
            if (sym.getType().getDimension() - node.getExps().size() > 0) subs.addLast(new IntConstant(0));
            if (subs.isEmpty()) lValueIRRes = arr;
            else {
                res = new GetelementptrInstr(Utils.getIncCounter(),
                        GetelementptrInstr.resolveArrayUnwrap(arr.getValueType(), subs.size()),
                        arr, subs.toArray(new Value[0]));
                curIRBasicBlock.appendInstruction(res);
                lValueIRRes = res;
            }
        }
        return sym;
    }

    private static void visitStmt(Stmt node) {
        final Symbol sym;
        switch (node.getLexType()) {
            case 1: // LVal = Exp; ! keep an eye on ForStmt, it has the same logic !
                sym = visitLVal(node.getLVal1());
                if (sym == null) break;
                if (sym.getType().isConst())
                    CompError.appendError(node.getLVal1().getIdent().getLineNum(), 'h', "changing constant " + node.getLVal1().getIdent().getValue());
                else {
                    final var tlv = lValueIRRes;
                    final var instr = new StoreInstr(visitExp(node.getExp1()).getResIR(), tlv);
                    curIRBasicBlock.appendInstruction(instr);
                }
                break;
            case 2: // Exp;
                if (node.getExp2() != null) visitExp(node.getExp2());
                break;
            case 3: // Block
                visitBlock(node.getBlock(), true);
                break;
            case 4: // if
                ifBBS.add(new BasicBlock(-1)); // for next normal statement
                if (node.getElseStmt() != null) ifBBS.add(new BasicBlock(-1)); // for else statement if exists
                ifBBS.add(new BasicBlock(-1)); // for if statement
                ifTrueBasicBlock = ifBBS.getLast();
                ifFalseBasicBlock = ifBBS.get(ifBBS.size() - 2);
                visitCond(node.getCond4());
                curIRBasicBlock = ifBBS.pollLast();
                curIRBasicBlock.setId(Utils.getIncCounter());
                curIRFunction.appendBasicBlock(curIRBasicBlock);
                visitStmt(node.getIfStmt());
                curIRBasicBlock.appendInstruction(new BrInstr(node.getElseStmt() != null ? ifBBS.get(ifBBS.size() - 2) : ifBBS.getLast()));
                if (node.getElseStmt() != null) {
                    curIRBasicBlock = ifBBS.pollLast();
                    curIRBasicBlock.setId(Utils.getIncCounter());
                    curIRFunction.appendBasicBlock(curIRBasicBlock);
                    visitStmt(node.getElseStmt());
                    curIRBasicBlock.appendInstruction(new BrInstr(ifBBS.getLast()));
                }
                curIRBasicBlock = ifBBS.pollLast();
                curIRBasicBlock.setId(Utils.getIncCounter());
                curIRFunction.appendBasicBlock(curIRBasicBlock);
                break;
            case 5: // for
                if (node.getInitStmt() != null) visitForStmt(node.getInitStmt());
                forBBS.add(new BasicBlock(-1));
                curIRBasicBlock.appendInstruction(new BrInstr(forBBS.getLast()));
                curIRBasicBlock = forBBS.getLast();
                curIRBasicBlock.setId(Utils.getIncCounter());
                curIRFunction.appendBasicBlock(curIRBasicBlock);
                if (node.getLoopStmt() != null) forBBS.add(new BasicBlock(-1));
                forBBS.add(new BasicBlock(-1)); forBBS.add(new BasicBlock(-1));
                ifTrueBasicBlock = forBBS.getLast();
                ifFalseBasicBlock = forBBS.get(forBBS.size() - 2);
                if (node.getCond5() != null) visitCond(node.getCond5());
                else curIRBasicBlock.appendInstruction(new BrInstr(ifTrueBasicBlock));
                curIRBasicBlock = forBBS.getLast();
                curIRBasicBlock.setId(Utils.getIncCounter());
                curIRFunction.appendBasicBlock(curIRBasicBlock);
                loopCount++;
                visitStmt(node.getStmt5());
                curIRBasicBlock.appendInstruction(new BrInstr(forBBS.get(forBBS.size() - 3)));
                loopCount--;
                if (node.getLoopStmt() != null) {
                    curIRBasicBlock = forBBS.get(forBBS.size() - 3);
                    curIRBasicBlock.setId(Utils.getIncCounter());
                    curIRFunction.appendBasicBlock(curIRBasicBlock);
                    visitForStmt(node.getLoopStmt());
                    curIRBasicBlock.appendInstruction(new BrInstr(forBBS.get(forBBS.size() - 4)));
                }
                curIRBasicBlock = forBBS.get(forBBS.size() - 2);
                curIRBasicBlock.setId(Utils.getIncCounter());
                curIRFunction.appendBasicBlock(curIRBasicBlock);
                forBBS.pollLast(); forBBS.pollLast();
                if (node.getLoopStmt() != null) forBBS.pollLast();
                forBBS.pollLast();
                break;
            case 6: // break or continue
                if (loopCount <= 0) {
                    CompError.appendError(node.getbOrC().getLineNum(), 'm', "using break/continue outside loop block");
                } else {
                    if (node.getbOrC().getType() == Token.Type.BREAKTK) curIRBasicBlock.appendInstruction(new BrInstr(forBBS.get(forBBS.size() - 2)));
                    else curIRBasicBlock.appendInstruction(new BrInstr(forBBS.get(forBBS.size() - 3)));
                    curIRBasicBlock = new BasicBlock(Utils.getIncCounter());
                    curIRFunction.appendBasicBlock(curIRBasicBlock);
                }
                break;
            case 7: // return
                if (node.getExp7() != null) {
                    if (isVoidFunction)
                        CompError.appendError(node.getExtraToken().getLineNum(), 'f', "return carrying value in void function");
                    else {
                        final var info = visitExp(node.getExp7());
                        if (info.isConst()) curIRBasicBlock.appendInstruction(new RetInstr(new IntConstant(info.getValue())));
                        else curIRBasicBlock.appendInstruction(new RetInstr(info.getResIR()));
                    }
                } else curIRBasicBlock.appendInstruction(new RetInstr());
                curIRBasicBlock = new BasicBlock(Utils.getIncCounter());
                curIRFunction.appendBasicBlock(curIRBasicBlock);
                break;
            case 8: // LVal = getint();
                sym = visitLVal(node.getLVal8());
                if (sym == null) break;
                if (sym.getType().isConst())
                    CompError.appendError(node.getLVal8().getIdent().getLineNum(), 'h', "reading into constant " + node.getLVal8().getIdent().getValue());
                else {
                    final var tlv = lValueIRRes;
                    Instruction instr = new CallInstr(Utils.getIncCounter(), exGetint, List.of());
                    curIRBasicBlock.appendInstruction(instr);
                    instr = new StoreInstr(instr, tlv);
                    curIRBasicBlock.appendInstruction(instr);
                }
                break;
            case 9: // printf();
                final var fmtCnt = Utils.substrCount(node.getFmtStr(), "%d");
                final var infoList = new ArrayList<ExpInfo>();
                if (fmtCnt != node.getExps9().size())
                    CompError.appendError(node.getExtraToken().getLineNum(), 'l', "unmatched format token count in printf");
                else {
                    for(var exp : node.getExps9()) infoList.add(visitExp(exp));
                    final var fmtStr = node.getFmtStr();
                    int i=0, j=0;
                    while (i<fmtStr.length()) {
                        char c = fmtStr.charAt(i);
                        if (c == '%') {
                            if (j >= infoList.size()) break; // must be error, stop gen
                            curIRBasicBlock.appendInstruction(new CallInstr(exPutint, List.of(infoList.get(j++).getResIR())));
                            i+=2;
                        } else if (c == '\\') {
                            curIRBasicBlock.appendInstruction(new CallInstr(exPutch, List.of(new IntConstant('\n'))));
                            i+=2;
                        } else {
                            curIRBasicBlock.appendInstruction(new CallInstr(exPutch, List.of(new IntConstant(c))));
                            i++;
                        }
                    }
                }
                break;
        }
    }

    private static void visitForStmt(ForStmt node) {
        // the same to Stmt lexical rule 1
        final var sym = visitLVal(node.getLVal());
        if (sym == null) return;
        if (sym.getType().isConst())
            CompError.appendError(node.getLVal().getIdent().getLineNum(), 'h', "changing constant " + node.getLVal().getIdent().getValue());
        else {
            final var tlv = lValueIRRes;
            final var instr = new StoreInstr(visitExp(node.getExp()).getResIR(), tlv);
            curIRBasicBlock.appendInstruction(instr);
        }
    }

    private static void visitExp(LOrExp node) {
        if (node.getLExp() != null) { // Handle left LOrExp and short-path
            ifBBS.add(ifFalseBasicBlock);
            ifFalseBasicBlock = new BasicBlock(-1);
            visitExp(node.getLExp());
            ifFalseBasicBlock.setId(Utils.getIncCounter());
            curIRBasicBlock = ifFalseBasicBlock;
            curIRFunction.appendBasicBlock(curIRBasicBlock);
            ifFalseBasicBlock = ifBBS.pollLast();
        }
        visitExp(node.getRExp());
    }

    private static void visitExp(LAndExp node) {
        if (node.getLExp() != null) { // Handle left LAndExp and short-path
            ifBBS.add(ifTrueBasicBlock);
            ifTrueBasicBlock = new BasicBlock(-1);
            visitExp(node.getLExp());
            ifTrueBasicBlock.setId(Utils.getIncCounter());
            curIRBasicBlock = ifTrueBasicBlock;
            curIRFunction.appendBasicBlock(curIRBasicBlock);
            ifTrueBasicBlock = ifBBS.pollLast();
        }
        final ExpInfo info;
        info = visitExp(node.getRExp());
        if (info.isConst()) curIRBasicBlock.appendInstruction(new BrInstr(info.getValue() != 0 ? ifTrueBasicBlock : ifFalseBasicBlock));
        else if (!info.isBool()) {
            final var instr = new IcmpInstr(IcmpInstr.Type.NE, Utils.getIncCounter(), info.getResIR(), new IntConstant(0));
            curIRBasicBlock.appendInstruction(instr);
            curIRBasicBlock.appendInstruction(new BrInstr(instr, ifTrueBasicBlock, ifFalseBasicBlock));
        } else curIRBasicBlock.appendInstruction(new BrInstr(info.getResIR(), ifTrueBasicBlock, ifFalseBasicBlock));
    }

    private static ExpInfo visitExp(EqExp node) {
        ExpInfo info1, info2;
        if (node.getOp() == null) { // single RelExp
            return visitExp(node.getRExp());
        } else { // EqExp == RelExp
            info1 = visitExp(node.getLExp());
            info2 = visitExp(node.getRExp());
        }
        if (info1.isConst() && info2.isConst()) { // constant optimization
            if (node.getOp().getType() == Token.Type.EQL) return new ExpInfo(true, info1.getValue().equals(info2.getValue()) ? 1 : 0, 0);
            else /* NEQ */ return new ExpInfo(true, info1.getValue().equals(info2.getValue()) ? 0 : 1, 0);
        } else {
            Instruction instr;
            if (!info1.isConst() && info1.isBool()) {
                instr = new ZextInstr(IntType.INT, Utils.getIncCounter(), info1.getResIR());
                curIRBasicBlock.appendInstruction(instr);
                info1 = new ExpInfo(false, 0, instr);
            }
            if (!info2.isConst() && info2.isBool()) {
                instr = new ZextInstr(IntType.INT, Utils.getIncCounter(), info2.getResIR());
                curIRBasicBlock.appendInstruction(instr);
                info2 = new ExpInfo(false, 0, instr);
            }
            instr = new IcmpInstr(
                    node.getOp().getType() == Token.Type.EQL ? IcmpInstr.Type.EQ : IcmpInstr.Type.NE,
                    Utils.getIncCounter(), info1.getResIR(), info2.getResIR());
            curIRBasicBlock.appendInstruction(instr);
            return new ExpInfo(true, 0, instr);
        }
    }

    private static ExpInfo visitExp(RelExp node) {
        ExpInfo info1, info2;
        if (node.getOp() == null) { // single AddExp
            return visitExp(node.getRExp());
        } else { // RelExp >= AddExp
            info1 = visitExp(node.getLExp());
            info2 = visitExp(node.getRExp());
        }
        if (info1.isConst() && info2.isConst()) { // constant optimization
            if (node.getOp().getType() == Token.Type.GRE) return new ExpInfo(true, info1.getValue() > info2.getValue() ? 1 : 0, 0);
            else if (node.getOp().getType() == Token.Type.GEQ) return new ExpInfo(true, info1.getValue() >= info2.getValue() ? 1 : 0, 0);
            else if (node.getOp().getType() == Token.Type.LSS) return new ExpInfo(true, info1.getValue() < info2.getValue() ? 1 : 0, 0);
            else /* LEQ */ return new ExpInfo(true, info1.getValue() <= info2.getValue() ? 1 : 0, 0);
        } else {
            Instruction instr;
            if (!info1.isConst() && info1.isBool()) {
                instr = new ZextInstr(IntType.INT, Utils.getIncCounter(), info1.getResIR());
                curIRBasicBlock.appendInstruction(instr);
                info1 = new ExpInfo(false, 0, instr);
            }
            instr = new IcmpInstr(
                    node.getOp().getType() == Token.Type.GRE ? IcmpInstr.Type.SGT :
                            node.getOp().getType() == Token.Type.GEQ ? IcmpInstr.Type.SGE :
                                    node.getOp().getType() == Token.Type.LSS ? IcmpInstr.Type.SLT : IcmpInstr.Type.SLE,
                    Utils.getIncCounter(), info1.getResIR(), info2.getResIR());
            curIRBasicBlock.appendInstruction(instr);
            return new ExpInfo(true, 0, instr);
        }
    }

    private static void visitCond(Cond node) {
        visitExp(node.getExp());
    }
}
