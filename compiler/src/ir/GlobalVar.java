package ir;

import ir.type.ArrayType;
import ir.type.PointerType;
import ir.type.ValueType;

import java.util.List;

public class GlobalVar extends User {
    private final Integer initValue;
    private final List<Integer> array1;
    private final List<List<Integer>> array2;
    // GlobalVar has pointer type

    public GlobalVar(String name, ValueType type, int initValue) {
        super(name, new PointerType(type));
        this.initValue = initValue;
        this.array1 = null;
        this.array2 = null;
    }
    public GlobalVar(String name, ValueType type, List<Integer> initArray1) {
        super(name, new PointerType(type));
        this.initValue = null;
        if (initArray1.size() > 0)
            if (initArray1.parallelStream().allMatch((e) -> e == 0))
                initArray1.clear();
        this.array1 = initArray1;
        this.array2 = null;
    }
    public GlobalVar(String name, ValueType type, List<List<Integer>> initArray2, boolean reserved) {
        super(name, new PointerType(type));
        this.initValue = null;
        this.array1 = null;
        if (initArray2.size() > 0)
            for (final var l : initArray2)
                if (l.parallelStream().allMatch((e) -> e == 0))
                    l.clear();
        this.array2 = initArray2;
    }

    @Override
    public void emitString(StringBuilder sb) {
        sb.append('@').append(name).append(" = dso_local global ");
        ((PointerType)type).getDeref().emitString(sb);
        sb.append(' ');
        if (initValue != null) sb.append(initValue);
        else if (array1 != null) emitArrayInitValue(sb, array1);
        else {
            if (array2.size() == 0) sb.append("zeroinitializer");
            else {
                final var t = ((ArrayType)((PointerType)type).getDeref()).getElementType();
                sb.append('['); t.emitString(sb); sb.append(' '); emitArrayInitValue(sb, array2.get(0));
                for (int i = 1; i < array2.size(); i++){
                    sb.append(", "); t.emitString(sb); sb.append(' '); emitArrayInitValue(sb, array2.get(i));
                }
                sb.append(']');
            }
        }
    }

    private static void emitArrayInitValue(StringBuilder sb, List<Integer> list) {
        if (list.size() == 0) sb.append("zeroinitializer");
        else {
            sb.append("[i32 ").append(list.get(0));
            for (int i = 1; i < list.size(); i++) sb.append(", i32 ").append(list.get(i));
            sb.append(']');
        }
    }
}
