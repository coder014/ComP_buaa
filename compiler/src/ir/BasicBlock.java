package ir;

import ir.instr.Instruction;
import ir.type.LabelType;

import java.util.ArrayList;
import java.util.List;

public class BasicBlock extends Value {
    private final List<Instruction> instructions = new ArrayList<>();

    public BasicBlock(int id) {
        super("L" + id, id, LabelType.INSTANCE);
    }

    public void appendInstruction(Instruction instr) {
        instructions.add(instr);
    }

    @Override
    public void emitString(StringBuilder sb) {
        sb.append(getId()).append(":\n");
        for (final var instr : instructions) {
            instr.emitString(sb);
            sb.append('\n');
        }
    }

    @Override
    public void setId(int id) {
        this.id = id;
        this.name = "L" + id;
    }
}
