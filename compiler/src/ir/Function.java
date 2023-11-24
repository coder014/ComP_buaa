package ir;

import compiler.Utils;
import ir.type.FunctionType;

import java.util.ArrayList;
import java.util.List;

public class Function extends User {
    private final List<FuncParam> params = new ArrayList<>();
    private final List<BasicBlock> blocks = new ArrayList<>();

    public Function(String name, FunctionType valueType) {
        super(name, valueType);
        final var paramType = valueType.getParamType();
        for (var pt : paramType) {
            params.add(new FuncParam(Utils.getIncCounter(), pt));
        }
    }

    public List<FuncParam> getParams() {
        return params;
    }

    public void appendBasicBlock(BasicBlock block) {
        blocks.add(block);
    }

    @Override
    public void emitString(StringBuilder sb) {
        sb.append("define dso_local ");
        ((FunctionType)getValueType()).getRetType().emitString(sb);
        sb.append(" @").append(getName()).append('(');
        for (int i = 0; i < params.size(); i++) {
            if (i > 0) sb.append(", ");
            params.get(i).getValueType().emitString(sb);
            sb.append(" %").append(params.get(i).getId());
        }
        sb.append(") {\n");
        for (final var bb : blocks) bb.emitString(sb);
        sb.append("}");
    }
}
