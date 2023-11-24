package compiler;

import ir.BasicBlock;
import ir.FuncParam;
import ir.Function;
import ir.GlobalVar;
import ir.IntConstant;
import ir.Module;
import ir.Value;
import ir.instr.AllocaInstr;
import ir.instr.ArithmeticInstr;
import ir.instr.CallInstr;
import ir.instr.Instruction;
import ir.instr.LoadInstr;
import ir.instr.RetInstr;
import ir.instr.StoreInstr;
import ir.type.FunctionType;
import ir.type.IntType;
import ir.type.ValueType;
import ir.type.VoidType;
import nonterm.*;
import symbol.CompError;
import symbol.Symbol;
import symbol.SymbolTable;
import symbol.SymbolType;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Visitor {
    private static int loopCount = 0;
    private static boolean isVoidFunction = false;
    private static Function curIRFunction;
    private static BasicBlock curIRBasicBlock;
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
                    irPType = null; // todo: array param as pointer
                } else { // 2-array pointer
                    pType = new SymbolType(false, List.of(0, 0));
                    irPType = null; // todo: array param as pointer
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
        if (!isVoidFunction && !funcBlockHasEndingReturn(node.getBlock()))
            CompError.appendError(block.getEndingLineNum(), 'g', "Missing ending return for function with ret-value");
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
        /* Assuming no errors in ConstExp
         for (var dimExp : node.getExps()) {
             // check expression only
             visitExp(dimExp);
         }
         * Assuming no errors in InitVal
         visitInitVal(node.getVal());
         */
        final Symbol sym;
        ExpInfo info;
        ConstInitVal initVal = node.getVal();
        // todo: treat const array as variable
        if (node.getExps().isEmpty()) { // constant
            sym = new Symbol(node.getIdent().getValue(), new SymbolType(true));
            info = visitExp(initVal.getExp());
            sym.setIntValue(info.getValue());
        } else if (node.getExps().size() == 1) { // const 1-array
            sym = new Symbol(node.getIdent().getValue(), new SymbolType(true, List.of(0)));
        } else { // const 2-array
            sym = new Symbol(node.getIdent().getValue(), new SymbolType(true, List.of(0, 0)));
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
        /* Assuming no errors in ConstExp
         for (var dimExp : node.getExps()) {
             // check expression only
             visitExp(dimExp);
         }
         * Assuming no errors in InitVal
         if (node.getVal() != null)
             visitInitVal(node.getVal());
         */
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
            sym = new Symbol(node.getIdent().getValue(), new SymbolType(false, List.of(0)));
        } else { // 2-array
            sym = new Symbol(node.getIdent().getValue(), new SymbolType(false, List.of(0, 0)));
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
            if (node.getOp().getType() == Token.Type.MINU) return new ExpInfo(info1.getValue() - info2.getValue(), info1.getDimension());
            else return new ExpInfo(info1.getValue() + info2.getValue(), info1.getDimension());
        } else {
            final ArithmeticInstr instr = new ArithmeticInstr(
                    node.getOp().getType() == Token.Type.MINU ? ArithmeticInstr.Type.SUB : ArithmeticInstr.Type.ADD,
                    Utils.getIncCounter(), info1.getResIR(), info2.getResIR());
            curIRBasicBlock.appendInstruction(instr);
            return new ExpInfo(info1.getDimension(), instr);
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
            if (node.getOp().getType() == Token.Type.MULT) return new ExpInfo(info1.getValue() * info2.getValue(), info1.getDimension());
            else if (node.getOp().getType() == Token.Type.DIV) return new ExpInfo(info1.getValue() / info2.getValue(), info1.getDimension());
            else return new ExpInfo(info1.getValue() % info2.getValue(), info1.getDimension());
        } else {
            final ArithmeticInstr instr = new ArithmeticInstr(
                    node.getOp().getType() == Token.Type.MULT ? ArithmeticInstr.Type.MUL :
                    node.getOp().getType() == Token.Type.DIV ? ArithmeticInstr.Type.SDIV : ArithmeticInstr.Type.SREM,
                    Utils.getIncCounter(), info1.getResIR(), info2.getResIR());
            curIRBasicBlock.appendInstruction(instr);
            return new ExpInfo(info1.getDimension(), instr);
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
                return new ExpInfo(null, -1);
            }
            if (sym.getType().getCategory() != SymbolType.Category.FUNC) return new ExpInfo(null, -1); // shall be function call
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
            info = new ExpInfo(sym.getType().getRetType() == Token.Type.VOIDTK ? -1 : 0, call);
        } else { // -UnaryExp
            info = visitExp(node.getUExp());
            if (info.isConst()) { // constant optimization
                if (node.getUOp().getType() == Token.Type.MINU) info = new ExpInfo(-info.getValue(), info.getDimension());
            } else if (node.getUOp().getType() == Token.Type.MINU) {
                final ArithmeticInstr instr = new ArithmeticInstr(ArithmeticInstr.Type.SUB,
                        Utils.getIncCounter(), new IntConstant(0), info.getResIR());
                curIRBasicBlock.appendInstruction(instr);
                info = new ExpInfo(info.getDimension(), instr);
            }
        }
        return info;
    }

    private static void visitExp(LOrExp node) {
        if (node.getLExp() == null) { // single LAndExp
            visitExp(node.getRExp());
        } else { // LOrExp || LAndExp
            visitExp(node.getLExp());
            visitExp(node.getRExp());
        }
    }

    private static void visitExp(LAndExp node) {
        if (node.getLExp() == null) { // single EqExp
            visitExp(node.getRExp());
        } else { // LAndExp && EqExp
            visitExp(node.getLExp());
            visitExp(node.getRExp());
        }
    }

    private static void visitExp(EqExp node) {
        if (node.getOp() == null) { // single RelExp
            visitExp(node.getRExp());
        } else { // EqExp == RelExp
            visitExp(node.getLExp());
            visitExp(node.getRExp());
        }
    }

    private static void visitExp(RelExp node) {
        if (node.getOp() == null) { // single AddExp
            visitExp(node.getRExp());
        } else { // RelExp >= AddExp
            visitExp(node.getLExp());
            visitExp(node.getRExp());
        }
    }

    private static ExpInfo visitExp(PrimaryExp node) {
        final ExpInfo info;
        if (node.getNumber() != null) { // Number
            info = new ExpInfo(node.getNumber().getIntValue(), 0);
        } else if (node.getLVal() != null) { // LVal
            final var sym = visitLVal(node.getLVal());
            if (sym == null) info = new ExpInfo(null, -1); // error: missing symbol
            else if (lValueIRRes instanceof IntConstant) { // const variable
                info = new ExpInfo(((IntConstant)lValueIRRes).getValue(), 0); // array pointer dereference
            } else {
                if (sym.getType().getDimension() - node.getLVal().getExps().size() == 0) { // int
                    final var instr = new LoadInstr(Utils.getIncCounter(), lValueIRRes);
                    curIRBasicBlock.appendInstruction(instr);
                    info = new ExpInfo(0, instr);
                } else { // array pointer
                    info = new ExpInfo(null, sym.getType().getDimension() - node.getLVal().getExps().size());
                }
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
        // todo: handle array ptr LVal
        for (var exp : node.getExps()) visitExp(exp);
        if (sym.getType().getDimension() == 0) {
            if (sym.getType().isConst()) { // basic const
                lValueIRRes = new IntConstant(sym.getIntValue());
            } else {
                lValueIRRes = sym.getIrValue();
            }
        }
        return sym;
    }

    private static void visitStmt(Stmt node) {
        final Symbol sym;
        switch (node.getLexType()) {
            case 1: // LVal = Exp;
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
                visitCond(node.getCond4());
                visitStmt(node.getIfStmt());
                if (node.getElseStmt() != null) visitStmt(node.getElseStmt());
                break;
            case 5: // for
                if (node.getInitStmt() != null) visitForStmt(node.getInitStmt());
                if (node.getCond5() != null) visitCond(node.getCond5());
                if (node.getLoopStmt() != null) visitForStmt(node.getLoopStmt());
                loopCount++;
                visitStmt(node.getStmt5());
                loopCount--;
                break;
            case 6: // break or continue
                if (loopCount <= 0) {
                    CompError.appendError(node.getbOrC().getLineNum(), 'm', "using break/continue outside loop block");
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
                            curIRBasicBlock.appendInstruction(new CallInstr(exPutint, List.of(infoList.get(j++).getResIR())));
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
        final var sym = visitLVal(node.getLVal());
        if (sym == null) return;
        if (sym.getType().isConst())
            CompError.appendError(node.getLVal().getIdent().getLineNum(), 'h', "changing constant " + node.getLVal().getIdent().getValue());
        else
            visitExp(node.getExp());
    }

    private static void visitCond(Cond node) {
        visitExp(node.getExp());
    }
}
