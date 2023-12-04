package ir.instr;

import ir.GlobalVar;
import ir.IntConstant;
import ir.Value;
import ir.type.ArrayType;
import ir.type.PointerType;
import ir.type.ValueType;

import java.util.List;

public class GetelementptrInstr extends Instruction {
    private final Value refArray; // must be a pointer
    private final List<Value> subs;
    public GetelementptrInstr(int dstId, ValueType dstType, Value refArray, Value... subs) {
        super(dstId, dstType);
        this.refArray = refArray;
        this.subs = List.of(subs);
    }

    public static ValueType resolveArrayUnwrap(ValueType toRes, int count) {
        for (int i = 0; i < count; i++) {
            if (toRes instanceof PointerType) toRes = ((PointerType)toRes).getDeref();
            else toRes = ((ArrayType)toRes).getElementType();
        }
        return new PointerType(toRes);
    }

    @Override
    public void emitString(StringBuilder sb) {
        super.emitString(sb);
        sb.append('%').append(id).append(" = getelementptr ");
        (((PointerType)(refArray.getValueType())).getDeref()).emitString(sb); sb.append(", ");
        refArray.getValueType().emitString(sb); sb.append(' ');
        if (refArray instanceof GlobalVar) sb.append('@').append(refArray.getName());
        else sb.append('%').append(refArray.getId());
        for (final var sub : subs) {
            sb.append(", i32 ");
            if (sub instanceof IntConstant) sub.emitString(sb);
            else sb.append('%').append(sub.getId());
        }
    }
}
