package nonterm;

import compiler.ParseState;
import compiler.Token;

import java.util.ArrayList;
import java.util.List;

public class FuncFParams {
    private final List<FuncFParam> params;

    private FuncFParams(FuncFParam param) {
        params = new ArrayList<>();
        params.add(param);
    }

    public List<FuncFParam> getParamList() {
        return params;
    }

    private void appendParam(FuncFParam param) {
        params.add(param);
    }

    public static FuncFParams parse(ParseState state) {
        final FuncFParams res = new FuncFParams(FuncFParam.parse(state));
        while (state.getCurToken().getType() == Token.Type.COMMA) {
                        state.nextToken();
            res.appendParam(FuncFParam.parse(state));
        }
                return res;
    }

    public static final String TYPESTR = "<FuncFParams>";
}
