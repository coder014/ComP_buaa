package compiler;

import java.io.IOException;
import java.io.PushbackInputStream;
import java.util.HashMap;
import java.util.Map;

public class Lexer {
    private final PushbackInputStream source;
    private int lineNum = 1;
    public static final Map<String, Token.Type> keywordMap = new HashMap<>();

    static {
        for (var type : Token.Type.values()) {
            String ts = type.toString().toLowerCase();
            if (ts.endsWith("tk"))
                keywordMap.put(ts.substring(0, ts.length() - 2), type);
        }
    }

    public Lexer(PushbackInputStream stream) {
        this.source = stream;
    }

    public boolean hasNext() {
        boolean inLineComment = false, inBlockComment = false;
        Character c = getc();
        while (c != null) {
            if (c.equals('\r')) {
                lineNum++;
                inLineComment = false;
                var tc = peek();
                if (tc == null) return false;
                if (tc.equals('\n')) {
                    getc();
                }
            } else if (c.equals('\n')) {
                lineNum++;
                inLineComment = false;
            } else if (inBlockComment) {
                if (c.equals('*')) {
                    var tc = peek();
                    if (tc == null) return false;
                    if (tc.equals('/')) {
                        inBlockComment = false;
                        getc();
                    }
                }
            } else if (!inLineComment && c.equals('/')) {
                var tc = peek();
                if (tc == null) {
                    ungetc(c);
                    return true;
                } else if (tc.equals('*')) {
                    inBlockComment = true;
                    getc();
                } else if (tc.equals('/')) {
                    inLineComment = true;
                    getc();
                } else {
                    ungetc(c);
                    return true;
                }
            } else if (!inLineComment && !c.equals(' ') && !c.equals('\t')) {
                ungetc(c);
                return true;
            }
            c = getc();
        }
        return false;
    }

    public Token next() {
        Character c = getc(), tc;
        if (c == null) {
            Utils.logErrorf("compiler.Lexer got unexpected EOF\n");
            return null;
        } else if (c.equals('\"')) {
            return dealStr();
        } else if (c.equals('!')) {
            tc = peek();
            if (Character.valueOf('=').equals(tc)) {
                getc();
                return new Token(Token.Type.NEQ, "!=");
            }
            return new Token(Token.Type.NOT, "!");
        } else if (c.equals('&')) {
            tc = peek();
            if (Character.valueOf('&').equals(tc)) {
                getc();
                return new Token(Token.Type.AND, "&&");
            }
            Utils.logErrorf("compiler.Lexer got single `&` at line %d\n", lineNum);
            return null;
        } else if (c.equals('|')) {
            tc = peek();
            if (Character.valueOf('|').equals(tc)) {
                getc();
                return new Token(Token.Type.OR, "||");
            }
            Utils.logErrorf("compiler.Lexer got single `|` at line %d\n", lineNum);
            return null;
        } else if (c.equals('+')) {
            return new Token(Token.Type.PLUS, "+");
        } else if (c.equals('-')) {
            return new Token(Token.Type.MINU, "-");
        } else if (c.equals('*')) {
            return new Token(Token.Type.MULT, "*");
        } else if (c.equals('/')) {
            return new Token(Token.Type.DIV, "/");
        } else if (c.equals('%')) {
            return new Token(Token.Type.MOD, "%");
        } else if (c.equals('<')) {
            tc = peek();
            if (Character.valueOf('=').equals(tc)) {
                getc();
                return new Token(Token.Type.LEQ, "<=");
            }
            return new Token(Token.Type.LSS, "<");
        } else if (c.equals('>')) {
            tc = peek();
            if (Character.valueOf('=').equals(tc)) {
                getc();
                return new Token(Token.Type.GEQ, ">=");
            }
            return new Token(Token.Type.GRE, ">");
        } else if (c.equals('=')) {
            tc = peek();
            if (Character.valueOf('=').equals(tc)) {
                getc();
                return new Token(Token.Type.EQL, "==");
            }
            return new Token(Token.Type.ASSIGN, "=");
        } else if (c.equals(';')) {
            return new Token(Token.Type.SEMICN, ";");
        } else if (c.equals(',')) {
            return new Token(Token.Type.COMMA, ",");
        } else if (c.equals('(')) {
            return new Token(Token.Type.LPARENT, "(");
        } else if (c.equals(')')) {
            return new Token(Token.Type.RPARENT, ")");
        } else if (c.equals('[')) {
            return new Token(Token.Type.LBRACK, "[");
        } else if (c.equals(']')) {
            return new Token(Token.Type.RBRACK, "]");
        } else if (c.equals('{')) {
            return new Token(Token.Type.LBRACE, "{");
        } else if (c.equals('}')) {
            return new Token(Token.Type.RBRACE, "}");
        } else if ((c.compareTo('a') >= 0 && c.compareTo('z') <= 0)
                || (c.compareTo('A') >= 0 && c.compareTo('Z') <= 0)
                || c.equals('_')) {
            return dealIdent(c);
        } else if (c.compareTo('0') >= 0 && c.compareTo('9') <= 0) {
            return dealInt(c);
        }
        Utils.logErrorf("compiler.Lexer got unexpected char `%c` at line %d\n", c, lineNum);
        return null;
    }

    private Token dealStr() {
        StringBuilder sb = new StringBuilder();
        while (true) {
            var tc = peek();
            if (tc == null) {
                Utils.logErrorf("compiler.Lexer got unexpected EOF\n");
                return null;
            }
            if (tc.compareTo(' ') < 0 || tc.compareTo('~') > 0) {
                Utils.logErrorf("compiler.Lexer got unexpected char(ascii %d) in a constant string at line %d\n", tc, lineNum);
                return null;
            }
            getc();
            if (tc.equals('\"')) break;
            sb.append(tc);
        }
        return new Token(Token.Type.STRCON, sb.toString());
    }

    private Token dealInt(char c) {
        StringBuilder sb = new StringBuilder().append(c);
        while (true) {
            var tc = peek();
            if (tc == null) break;
            if (tc.compareTo('0') < 0 || tc.compareTo('9') > 0) break;
            getc();
            sb.append(tc);
        }
        var res = sb.toString();
        if (res.length() > 1 && res.charAt(0) == '0')
            Utils.logWarnf("compiler.Lexer parsed a constant integer with leading zero at line %d\n", lineNum);
        return new Token(Token.Type.INTCON, res);
    }

    private Token dealIdent(char c) {
        StringBuilder sb = new StringBuilder().append(c);
        while (true) {
            var tc = peek();
            if (tc == null) break;
            if (tc.compareTo('0') < 0 || tc.compareTo('9') > 0
                    && (tc.compareTo('a') < 0 || tc.compareTo('z') > 0)
                    && (tc.compareTo('A') < 0 || tc.compareTo('Z') > 0)
                    && !tc.equals('_')) break;
            getc();
            sb.append(tc);
        }
        var res = sb.toString();
        var resType = keywordMap.get(res);
        if (resType == null) return new Token(Token.Type.IDENFR, res);
        return new Token(resType, res);
    }

    private void ungetc(char c) {
        try {
            source.unread(c);
        } catch (IOException e) {
            Utils.logErrorf("compiler.Lexer got: %s\n", e.getMessage());
        }
    }

    private Character getc() {
        try {
            var c = source.read();
            if (c == -1) {
                Utils.logDebugf("Input file reached its end.\n");
                return null;
            }
            return (char) c;
        } catch (IOException e) {
            Utils.logErrorf("compiler.Lexer got: %s\n", e.getMessage());
            return null;
        }
    }

    private Character peek() {
        try {
            var c = source.read();
            if (c == -1) {
                Utils.logDebugf("Input file reached its end.\n");
                return null;
            }
            source.unread(c);
            return (char) c;
        } catch (IOException e) {
            Utils.logErrorf("compiler.Lexer got: %s\n", e.getMessage());
            return null;
        }
    }
}
