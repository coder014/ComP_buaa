package compiler;

public class ExpInfo {
    private final boolean isConst;
    private final boolean isBool;
    private final Integer value;
    private final Boolean bool;
    private final Integer dimension;

    protected ExpInfo(Integer value, Integer dimension) {
        this.isConst = false;
        this.isBool = false;
        this.value = value;
        this.dimension = dimension;
        this.bool = false;
    }

    protected int getDimension() {
        return dimension;
    }
}
