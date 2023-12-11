package compiler;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public class RegisterAllocator {
    private final Map<Integer, Integer> vRegMap = new HashMap<>();
    private final Map<Integer, Integer> rRegMap = new HashMap<>();
    private final SortedSet<Integer> freeRRegs = new TreeSet<>(Integer::compareTo);
    private final Set<Integer> inMemVRegs = new HashSet<>();
    public RegisterAllocator() {
        freeRRegs.add(8); freeRRegs.add(9); freeRRegs.add(10); freeRRegs.add(11);
        freeRRegs.add(12); freeRRegs.add(13); freeRRegs.add(14); freeRRegs.add(15);
        freeRRegs.add(16); freeRRegs.add(17); freeRRegs.add(18); freeRRegs.add(19);
        freeRRegs.add(20); freeRRegs.add(21); freeRRegs.add(22); freeRRegs.add(23);
    }

    /*public int defRegisterAlloc(int vreg) {
        if (vRegMap.containsKey(vreg) || inMemVRegs.contains(vreg)) Utils.logErrorf("vreg %d has already defined", vreg);
        if (!freeRRegs.isEmpty()) { // have free mips regs
            final int rreg = freeRRegs.iterator().next();
            freeRRegs.remove(rreg);
            rRegMap.put(rreg, vreg);
            vRegMap.put(vreg, rreg);
            return rreg;
        }
        final int repReg = rRegMap.keySet().iterator().next();
        inMemVRegs.add(rRegMap.get(repReg));
        vRegMap.remove(rRegMap.get(repReg));
        rRegMap.put(repReg, vreg);
        vRegMap.put(vreg, repReg);
        return -repReg;
    }

    public int singleUseRegisterAlloc(int vreg) {
        if (!inMemVRegs.contains(vreg) && !vRegMap.containsKey(vreg)) Utils.logErrorf("vreg %d never defined or is already used", vreg);
        if (vRegMap.containsKey(vreg)) { // in real regs
            final var res = vRegMap.get(vreg);
            vRegMap.remove(vreg);
            rRegMap.remove(res);
            freeRRegs.add(res);
            return res;
        }
        inMemVRegs.remove(vreg);
        if (!freeRRegs.isEmpty()) { // have free mips regs
            return freeRRegs.iterator().next();
        } else {
            final int repReg = rRegMap.keySet().iterator().next();
            inMemVRegs.add(rRegMap.get(repReg));
            vRegMap.remove(rRegMap.get(repReg));
            rRegMap.remove(repReg);
            return -repReg;
        }
    }

    public int[] doubleUseRegisterAlloc(int vreg1, int vreg2) {
        if (!inMemVRegs.contains(vreg1) && !vRegMap.containsKey(vreg1)
                && !inMemVRegs.contains(vreg2) && !vRegMap.containsKey(vreg2)) Utils.logErrorf("vreg %d or %d never defined or is already used", vreg1, vreg2);
        final var res = new int[2];
        if (vRegMap.containsKey(vreg1) && vRegMap.containsKey(vreg2)) { // both in real regs
            res[0] = vRegMap.get(vreg1); res[1] = vRegMap.get(vreg2);
            vRegMap.remove(vreg1); vRegMap.remove(vreg2);
            rRegMap.remove(res[0]); rRegMap.remove(res[1]);
            freeRRegs.add(res[0]); freeRRegs.add(res[1]);
        } else if (vRegMap.containsKey(vreg1)) {
            if (!freeRRegs.isEmpty()) {
                res[0] = vRegMap.get(vreg1); res[1] = freeRRegs.iterator().next();
                vRegMap.remove(vreg1);
                rRegMap.remove(res[0]);
                freeRRegs.add(res[0]);
            } else {
                final int repReg = rRegMap.keySet().iterator().next();
                inMemVRegs.add(rRegMap.get(repReg));
                vRegMap.remove(rRegMap.get(repReg));
                rRegMap.remove(repReg);
                return -repReg;
            }
            res[1] = -res[1];
        } else if (vRegMap.containsKey(vreg2)) {

        } else {

        }
        return res;
    }*/
}
