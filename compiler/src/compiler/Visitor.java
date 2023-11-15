package compiler;

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
        if (!SymbolTable.getCurrent().appendSymbol(new Symbol(fIdent.getValue(), new SymbolType(fType, pTypeList)))) {
            CompError.appendError(fIdent.getLineNum(), 'b', "Duplicated definition for func " + fIdent.getValue());
        }
        SymbolTable.enterNewScope();
        if (node.getParams() != null) {
            final var params = node.getParams().getParamList();
            for (var param : params) {
                final Symbol sym;
                final SymbolType pType;
                // assuming param's dimension Exp is correct
                if (param.getExps().isEmpty()) { // variable
                    pType = new SymbolType(false);
                } else if (param.getExps().size() == 1) { // 1-array pointer
                    pType = new SymbolType(false, List.of(0));
                } else { // 2-array pointer
                    pType = new SymbolType(false, List.of(0, 0));
                }
                sym = new Symbol(param.getIdent().getValue(), pType);
                if (!SymbolTable.getCurrent().appendSymbol(sym)) {
                    CompError.appendError(param.getIdent().getLineNum(), 'b', "Duplicated definition for param " + param.getIdent().getValue());
                } else {
                    pTypeList.add(pType);
                }
            }
        }
        isVoidFunction = fType == Token.Type.VOIDTK;
        visitBlock(block, false);
        if (!isVoidFunction && !funcBlockHasEndingReturn(node.getBlock()))
            CompError.appendError(block.getEndingLineNum(), 'g', "Missing ending return for function with ret-value");
        SymbolTable.exitCurrentScope();
    }

    private static void visitFuncDef(MainFuncDef node) {
        final var sym = new Symbol("main", new SymbolType(Token.Type.INTTK, List.of()));
        SymbolTable.enterNewScope();
        isVoidFunction = false;
        visitBlock(node.getBlock(), false);
        if (!funcBlockHasEndingReturn(node.getBlock()))
            CompError.appendError(node.getBlock().getEndingLineNum(), 'g', "Missing ending return for main function");
        SymbolTable.exitCurrentScope();
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
        if (node.getExps().isEmpty()) { // constant
            sym = new Symbol(node.getIdent().getValue(), new SymbolType(true));
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
        if (node.getExps().isEmpty()) { // variable
            sym = new Symbol(node.getIdent().getValue(), new SymbolType(false));
        } else if (node.getExps().size() == 1) { // 1-array
            sym = new Symbol(node.getIdent().getValue(), new SymbolType(false, List.of(0)));
        } else { // 2-array
            sym = new Symbol(node.getIdent().getValue(), new SymbolType(false, List.of(0, 0)));
        }
        if (!SymbolTable.getCurrent().appendSymbol(sym)) {
            CompError.appendError(node.getIdent().getLineNum(), 'b', "Duplicated definition for ident " + node.getIdent().getValue());
        }
    }

    private static ExpInfo visitExp(Exp node) {
        final var info = visitExp(node.getExp());
        return new ExpInfo(null, info.getDimension());
    }

    private static ExpInfo visitExp(AddExp node) {
        final ExpInfo info;
        if (node.getOp() == null) { // single MulExp
            info = visitExp(node.getRExp());
        } else { // AddExp + MulExp
            visitExp(node.getLExp());
            info = visitExp(node.getRExp()); // assuming dimension is equal
        }
        return new ExpInfo(null, info.getDimension());
    }

    private static ExpInfo visitExp(MulExp node) {
        final ExpInfo info;
        if (node.getOp() == null) { // single UnaryExp
            info = visitExp(node.getRExp());
        } else { // MulExp * UnaryExp
            visitExp(node.getLExp());
            info = visitExp(node.getRExp()); // assuming dimension is equal
        }
        return new ExpInfo(null, info.getDimension());
    }

    private static ExpInfo visitExp(UnaryExp node) {
        final ExpInfo info;
        if (node.getPExp() != null) { // PrimaryExp
            info = visitExp(node.getPExp());
        } else if (node.getIdent() != null) { // func(call params)
            final var sym = SymbolTable.getSymbolByName(node.getIdent().getValue());
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
                    }
                }
            } else { // 0 param
                if (!sym.getType().getParams().isEmpty())
                    CompError.appendError(node.getIdent().getLineNum(), 'd', "unmatched params count for function call " + node.getIdent().getValue());
            }
            info = new ExpInfo(null, sym.getType().getRetType() == Token.Type.VOIDTK ? -1 : 0);
        } else { // -UnaryExp
            info = visitExp(node.getUExp());
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
            info = new ExpInfo(null, 0);
        } else if (node.getLVal() != null) { // LVal
            final var sym = visitLVal(node.getLVal());
            if (sym == null) info = new ExpInfo(null, -1); // If missing symbol, how to evaluation?
            else info = new ExpInfo(null, sym.getType().getDimension() - node.getLVal().getExps().size()); // array pointer dereference
        } else { // (exp)
            info = visitExp(node.getExp());
        }
        return info;
    }

    private static Symbol visitLVal(LVal node) {
        final var sym = SymbolTable.getSymbolByName(node.getIdent().getValue());
        if (sym == null) {
            CompError.appendError(node.getIdent().getLineNum(), 'c', "Undefined symbol " + node.getIdent().getValue());
            return null;
        }
        for (var exp : node.getExps()) visitExp(exp);
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
                else
                    visitExp(node.getExp1());
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
                    else
                        visitExp(node.getExp7());
                }
                break;
            case 8: // LVal = getint();
                sym = visitLVal(node.getLVal8());
                if (sym == null) break;
                if (sym.getType().isConst())
                    CompError.appendError(node.getLVal8().getIdent().getLineNum(), 'h', "reading into constant " + node.getLVal8().getIdent().getValue());
                break;
            case 9: // printf();
                final var fmtCnt = Utils.substrCount(node.getFmtStr(), "%d");
                if (fmtCnt != node.getExps9().size())
                    CompError.appendError(node.getExtraToken().getLineNum(), 'l', "unmatched format token count in printf");
                else for(var exp : node.getExps9()) visitExp(exp);
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
