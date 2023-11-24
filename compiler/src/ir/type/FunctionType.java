package ir.type;

import java.util.List;

public class FunctionType extends ValueType {
    private final ValueType retType;
    private final List<ValueType> paramType;

    public FunctionType(ValueType retType, List<ValueType> paramType) {
        super();
        this.retType = retType;
        this.paramType = paramType;
    }

    public ValueType getRetType() {
        return retType;
    }

    public List<ValueType> getParamType() {
        return paramType;
    }

    @Override
    public void emitString(StringBuilder sb) {

    }
}
