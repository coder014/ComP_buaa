package ir.instr;

import ir.Function;
import ir.IntConstant;
import ir.Value;
import ir.type.IntType;
import ir.type.VoidType;

import java.util.List;

public class CallInstr extends Instruction {
    private final Function toFunc;
    private final List<Value> args;

    public CallInstr(Function toFunc, List<Value> args) { // for void or ignored
        super(VoidType.INSTANCE);
        this.toFunc = toFunc;
        this.args = args;
    }
    public CallInstr(int dstId, Function toFunc, List<Value> args) {
        super("T" + dstId, dstId, IntType.INT);
        this.toFunc = toFunc;
        this.args = args;
    }

    @Override
    public void emitString(StringBuilder sb) {
        super.emitString(sb);
        if (type instanceof VoidType) {
            sb.append("call void @");
        } else {
            sb.append('%').append(id).append(" = call i32 @"); // must be i32
        }
        sb.append(toFunc.getName()).append('(');
        for (int i = 0; i < args.size(); i++) {
            if (i > 0) sb.append(", ");
            final var t = args.get(i);
            if (t instanceof IntConstant) {
                sb.append("i32 ");
                t.emitString(sb);
            } else {
                t.getValueType().emitString(sb);
                sb.append(" %").append(t.getId());
            }
        }
        sb.append(')');
    }
}