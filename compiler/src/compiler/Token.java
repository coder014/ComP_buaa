package compiler;

public class Token {
    private final Type type;
    private final String value;

    public Token(Type type, String value) {
        this.type = type;
        this.value = value;
    }

    public Type getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        if (type == Type.STRCON)
            return String.format("%s \"%s\"", type, value);
        else return String.format("%s %s", type, value);
    }

    enum Type {
        IDENFR, INTCON, STRCON,
        MAINTK, CONSTTK, INTTK, BREAKTK, CONTINUETK, IFTK,
        ELSETK, FORTK, GETINTTK, PRINTFTK, RETURNTK, VOIDTK,
        NOT, AND, OR, PLUS, MINU, MULT, DIV, MOD,
        LSS, LEQ, GRE, GEQ, EQL, NEQ,
        ASSIGN, SEMICN, COMMA, LPARENT, RPARENT,
        LBRACK, RBRACK, LBRACE, RBRACE
    }
}
