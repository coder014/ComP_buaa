package nonterm;

import compiler.ParseState;
import compiler.Token;
import compiler.Utils;
import symbol.CompError;

import java.util.ArrayList;
import java.util.List;

public class Stmt {
    private final LVal lVal1;
    private final Exp exp1;
    private final Exp exp2;
    private final Block block3;
    private final Cond cond4;
    private final Stmt ifStmt4;
    private final Stmt elseStmt4;
    private final ForStmt initStmt5;
    private final Cond cond5;
    private final ForStmt loopStmt5;
    private final Stmt stmt5;
    private final Token bOrC;
    private final Exp exp7;
    private final LVal lVal8;
    private final Token fmtStr9;
    private final List<Exp> exps9;
    private final int lexType;
    private final Token extraToken; // for printf or return

    private Stmt(LVal lVal1, Exp exp1) {
        this.lexType = 1;this.lVal1 = lVal1;this.exp1 = exp1;
        exp2=null;block3=null;cond4=null;this.extraToken=null;
        ifStmt4=null;elseStmt4=null;initStmt5=null;
        cond5=null;loopStmt5=null;stmt5=null;bOrC=null;
        exp7=null;lVal8=null;fmtStr9=null;exps9=null;
    }
    private Stmt(Exp exp) {
        this.exp2 = exp;this.exp7 = null;this.lexType = 2;
        lVal1=null;exp1=null;block3=null;cond4=null;
        ifStmt4=null;elseStmt4=null;initStmt5=null;
        cond5=null;loopStmt5=null;stmt5=null;bOrC=null;
        lVal8=null;fmtStr9=null;exps9=null;this.extraToken=null;
    }
    private Stmt(Block block3) {
        this.lexType = 3;this.block3 = block3;this.extraToken=null;
        lVal1=null;exp1=null;exp2=null;cond4=null;
        ifStmt4=null;elseStmt4=null;initStmt5=null;
        cond5=null;loopStmt5=null;stmt5=null;bOrC=null;
        exp7=null;lVal8=null;fmtStr9=null;exps9=null;
    }
    private Stmt(Cond cond4, Stmt ifStmt4, Stmt elseStmt4) {
        this.lexType = 4;this.cond4 = cond4;this.ifStmt4 = ifStmt4;
        this.elseStmt4 = elseStmt4;this.extraToken=null;
        lVal1=null;exp1=null;exp2=null;block3=null;initStmt5=null;
        cond5=null;loopStmt5=null;stmt5=null;bOrC=null;
        exp7=null;lVal8=null;fmtStr9=null;exps9=null;
    }
    private Stmt(ForStmt initStmt5, Cond cond5, ForStmt loopStmt5, Stmt stmt5) {
        this.lexType = 5;this.initStmt5 = initStmt5;this.cond5 = cond5;
        this.loopStmt5 = loopStmt5;this.stmt5 = stmt5;
        lVal1=null;exp1=null;exp2=null;block3=null;cond4=null;
        ifStmt4=null;elseStmt4=null;bOrC=null;this.extraToken=null;
        exp7=null;lVal8=null;fmtStr9=null;exps9=null;
    }
    private Stmt(Token bOrC) {
        this.lexType = 6;this.bOrC = bOrC;
        lVal1=null;exp1=null;exp2=null;block3=null;cond4=null;
        ifStmt4=null;elseStmt4=null;initStmt5=null;
        cond5=null;loopStmt5=null;stmt5=null;this.extraToken=null;
        exp7=null;lVal8=null;fmtStr9=null;exps9=null;
    }
    private Stmt(Token ret, Exp exp) {
        this.exp7 = exp;this.exp2 = null;this.lexType = 7;
        lVal1=null;exp1=null;block3=null;cond4=null;
        ifStmt4=null;elseStmt4=null;initStmt5=null;
        cond5=null;loopStmt5=null;stmt5=null;bOrC=null;
        lVal8=null;fmtStr9=null;exps9=null;this.extraToken=ret;
    }
    private Stmt(LVal lVal8) {
        this.lexType = 8;this.lVal8 = lVal8;
        lVal1=null;exp1=null;exp2=null;block3=null;cond4=null;
        ifStmt4=null;elseStmt4=null;initStmt5=null;
        cond5=null;loopStmt5=null;stmt5=null;bOrC=null;
        exp7=null;fmtStr9=null;exps9=null;this.extraToken=null;
    }
    private Stmt(Token print, Token fmtStr9, List<Exp> exps9) {
        this.lexType = 9;this.fmtStr9 = fmtStr9;this.exps9 = exps9;
        lVal1=null;exp1=null;exp2=null;block3=null;cond4=null;
        ifStmt4=null;elseStmt4=null;initStmt5=null;this.extraToken=print;
        cond5=null;loopStmt5=null;stmt5=null;bOrC=null;exp7=null;lVal8=null;
    }

    public LVal getLVal1() {
        return lVal1;
    }
    public Exp getExp1() {
        return exp1;
    }
    public Exp getExp2() {
        return exp2;
    }
    public Block getBlock() {
        return block3;
    }
    public Cond getCond4() {
        return cond4;
    }
    public Stmt getIfStmt() {
        return ifStmt4;
    }
    public Stmt getElseStmt() {
        return elseStmt4;
    }
    public ForStmt getInitStmt() {
        return initStmt5;
    }
    public Cond getCond5() {
        return cond5;
    }
    public ForStmt getLoopStmt() {
        return loopStmt5;
    }
    public Stmt getStmt5() {
        return stmt5;
    }
    public Token getbOrC() {
        return bOrC;
    }
    public Exp getExp7() {
        return exp7;
    }
    public LVal getLVal8() {
        return lVal8;
    }
    public String getFmtStr() {
        return fmtStr9.getValue();
    }
    public List<Exp> getExps9() {
        return exps9;
    }
    public int getLexType() {
        return lexType;
    }
    public Token getExtraToken() {
        return extraToken;
    }

    public static Stmt parse(ParseState state) {
        final Stmt res;
        if (state.getCurToken().getType() == Token.Type.LBRACE) {
            res = new Stmt(Block.parse(state));
        } else if (state.getCurToken().getType() == Token.Type.IFTK) {
            state.nextToken();
            state.nextToken();
            final var t1 = Cond.parse(state);
            if (state.getCurToken().getType() == Token.Type.RPARENT) {
                state.nextToken();
            } else {
                state.ungetToken();
                CompError.appendError(state.getCurToken().getLineNum(), 'j', "Missing `)` as if(Cond)");
                state.nextToken();
            }
            final var t2 = Stmt.parse(state);
            if (state.getCurToken().getType() == Token.Type.ELSETK) {
                state.nextToken();
                res = new Stmt(t1, t2, Stmt.parse(state));
            } else {
                res = new Stmt(t1, t2, null);
            }
        } else if (state.getCurToken().getType() == Token.Type.FORTK) {
            final ForStmt t1, t3;
            final Cond t2;
            state.nextToken();
            state.nextToken();
            if (state.getCurToken().getType() == Token.Type.SEMICN) t1 = null;
            else t1 = ForStmt.parse(state);
            state.nextToken();
            if (state.getCurToken().getType() == Token.Type.SEMICN) t2 = null;
            else t2 = Cond.parse(state);
            state.nextToken();
            if (state.getCurToken().getType() == Token.Type.RPARENT) t3 = null;
            else t3 = ForStmt.parse(state);
            state.nextToken();
            res = new Stmt(t1, t2, t3, Stmt.parse(state));
        } else if (state.getCurToken().getType() == Token.Type.BREAKTK
                || state.getCurToken().getType() == Token.Type.CONTINUETK) {
            res = new Stmt(state.getCurToken());
            final var lineNum = state.getCurToken().getLineNum();
            state.nextToken();
            if (state.getCurToken().getType() == Token.Type.SEMICN) {
                state.nextToken();
            } else {
                CompError.appendError(lineNum, 'i', "Missing `;` after token break/continue");
            }
        } else if (state.getCurToken().getType() == Token.Type.RETURNTK) {
            final var ret = state.getCurToken();
            state.nextToken();
            if (state.getCurToken().getType() == Token.Type.IDENFR
                    || state.getCurToken().getType() == Token.Type.PLUS
                    || state.getCurToken().getType() == Token.Type.MINU
                    || state.getCurToken().getType() == Token.Type.NOT
                    || state.getCurToken().getType() == Token.Type.LPARENT
                    || state.getCurToken().getType() == Token.Type.INTCON) {
                res = new Stmt(ret, Exp.parse(state));
            } else res = new Stmt(ret, null);
            if (state.getCurToken().getType() == Token.Type.SEMICN) {
                state.nextToken();
            } else {
                state.ungetToken();
                CompError.appendError(state.getCurToken().getLineNum(), 'i', "Missing `;` after return exp");
                state.nextToken();
            }
        } else if (state.getCurToken().getType() == Token.Type.PRINTFTK) {
            final var print = state.getCurToken();
            state.nextToken();
            state.nextToken();
            final var t1 = state.getCurToken();
            final var fmtStr = t1.getValue();
            for (int i=0; i<fmtStr.length(); i++) {
                var c = fmtStr.charAt(i);
                if (c != 32 && c != 33 && (c < 40 || c > 126)) {
                    if (c == '%' && i < fmtStr.length() - 1 && fmtStr.charAt(i+1) == 'd') i++;
                    else {
                        CompError.appendError(t1.getLineNum(), 'a', fmtStr);
                        break;
                    }
                } else if (c == '\\' && (i+1 >= fmtStr.length() || fmtStr.charAt(i+1) != 'n')) {
                    CompError.appendError(t1.getLineNum(), 'a', fmtStr);
                    break;
                }
            }
            final var t2 = new ArrayList<Exp>();
            state.nextToken();
            while (state.getCurToken().getType() == Token.Type.COMMA) {
                state.nextToken();
                t2.add(Exp.parse(state));
            }
            res = new Stmt(print, t1, t2);
            if (state.getCurToken().getType() == Token.Type.RPARENT) {
                state.nextToken();
            } else {
                state.ungetToken();
                CompError.appendError(state.getCurToken().getLineNum(), 'j', "Missing `)` as printf()");
                state.nextToken();
            }
            if (state.getCurToken().getType() == Token.Type.SEMICN) {
                state.nextToken();
            } else {
                state.ungetToken();
                CompError.appendError(state.getCurToken().getLineNum(), 'i', "Missing `;` as printf()");
                state.nextToken();
            }
        } else if (state.getCurToken().getType() == Token.Type.LPARENT
                || state.getCurToken().getType() == Token.Type.INTCON
                || state.getCurToken().getType() == Token.Type.PLUS
                || state.getCurToken().getType() == Token.Type.MINU
                || state.getCurToken().getType() == Token.Type.NOT) {
            res = new Stmt(Exp.parse(state));
            state.nextToken();
        } else if (state.getCurToken().getType() == Token.Type.SEMICN) { // rule 2, empty Exp
            res = new Stmt((Exp)null);
            state.nextToken();
        } else { // IDENT, may be rule 1, 2 or 8
            final Token t1, t2;
            t1 = state.getCurToken(); state.nextToken();
            t2 = state.getCurToken(); state.ungetToken();
            if (t1.getType() == Token.Type.IDENFR
                    && t2.getType() == Token.Type.LPARENT) { // rule 2
                res = new Stmt(Exp.parse(state));
                if (state.getCurToken().getType() == Token.Type.SEMICN) {
                    state.nextToken();
                } else {
                    state.ungetToken();
                    CompError.appendError(state.getCurToken().getLineNum(), 'i', "Missing `;` as Stmt -> [Exp];");
                    state.nextToken();
                }
            } else { // rule 1, 2 or 8
                // first parse the LVal
                state.startRecovery();
                final var tVal = LVal.parse(state);
                if (state.getCurToken().getType() == Token.Type.ASSIGN) {
                    // rule 1 or 8
                    state.abortRecovery();
                    state.nextToken();
                    if (state.getCurToken().getType() == Token.Type.GETINTTK) {
                        // rule 8
                        state.nextToken();
                        state.nextToken();
                        if (state.getCurToken().getType() == Token.Type.RPARENT) {
                            state.nextToken();
                        } else {
                            state.ungetToken();
                            CompError.appendError(state.getCurToken().getLineNum(), 'j', "Missing `)` as Stmt -> LVal = getint()");
                            state.nextToken();
                        }
                        if (state.getCurToken().getType() == Token.Type.SEMICN) {
                            state.nextToken();
                        } else {
                            state.ungetToken();
                            CompError.appendError(state.getCurToken().getLineNum(), 'i', "Missing `;` as Stmt -> LVal = getint()");
                            state.nextToken();
                        }
                        res = new Stmt(tVal);
                    } else { // rule 1
                        res = new Stmt(tVal, Exp.parse(state));
                        if (state.getCurToken().getType() == Token.Type.SEMICN) {
                            state.nextToken();
                        } else {
                            state.ungetToken();
                            CompError.appendError(state.getCurToken().getLineNum(), 'i', "Missing `;` as Stmt -> LVal = [Exp];");
                            state.nextToken();
                        }
                    }
                } else { // rule 2
                    state.doneRecovery();
                    res = new Stmt(Exp.parse(state));
                    if (state.getCurToken().getType() == Token.Type.SEMICN) {
                        state.nextToken();
                    } else {
                        state.ungetToken();
                        CompError.appendError(state.getCurToken().getLineNum(), 'i', "Missing `;` as Stmt -> [Exp];");
                        state.nextToken();
                    }
                }
            }
        }
        return res;
    }

    public static final String TYPESTR = "<Stmt>";
}
