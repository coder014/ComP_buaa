import compiler.Lexer;
import compiler.Utils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

public class Compiler {
    public static void main(String[] args) {
//        Utils.setLogLevel(Utils.LogLevel.DEBUG);
        var is = Utils.getFileAsStream("testfile.txt");
        var lexer = new Lexer(is);
        try {
            var out = new PrintStream(new FileOutputStream("output.txt"));
            while (lexer.hasNext()) {
                out.println(lexer.next());
            }
            out.close();
        } catch (IOException e) {
            Utils.logErrorf("write file error: %s\n", e.getMessage());
        }
    }
}
