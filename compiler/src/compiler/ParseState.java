package compiler;

import java.util.Deque;
import java.util.LinkedList;

public class ParseState {
    private final Lexer lexer;
    private final Deque<Token> buf = new LinkedList<>();
    private final Deque<Token> recoveryBuf = new LinkedList<>();
    private final Deque<Token> backBuf = new LinkedList<>();
    private final static int WINDOWSIZE = 4;
    private boolean inRecovery = false;

    public ParseState(Lexer lexer) {
        this.lexer = lexer;
    }

    public Token getCurToken() {
        return (inRecovery ? recoveryBuf : buf).peekLast();
    }

    public void nextToken() {
        final var writeBuf = inRecovery ? recoveryBuf : buf;
        if (!backBuf.isEmpty())
            writeBuf.add(backBuf.pollLast());
        else if (lexer.hasNext())
            writeBuf.add(lexer.next());
        // else error;
        if (buf.size() > WINDOWSIZE) buf.pollFirst();
    }

    public void ungetToken() {
        backBuf.add((inRecovery ? recoveryBuf : buf).pollLast());
    }

    public void startRecovery() {
        inRecovery = true;
        if (!buf.isEmpty()) recoveryBuf.add(buf.peekLast());
    }
    public void doneRecovery() {
        inRecovery = false;
        for (var it = recoveryBuf.descendingIterator(); it.hasNext();) {
            backBuf.add(it.next());
            it.remove();
        }
        if (!buf.isEmpty()) backBuf.pollLast();
    }
    public void abortRecovery() {
        inRecovery = false;
        if (!buf.isEmpty()) recoveryBuf.pollFirst();
        buf.addAll(recoveryBuf);
        recoveryBuf.clear();
        while (buf.size() > WINDOWSIZE) buf.pollFirst();
    }
}
