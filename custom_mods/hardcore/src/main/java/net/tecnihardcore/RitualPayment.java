package net.tecnihardcore;
import net.minecraft.nbt.*;
/** Exactly one debit, idempotent once the durable inventory loses its marker. */
public final class RitualPayment {
    public static boolean recover(NbtList inventory,String payment) {
        for(int i=inventory.size()-1;i>=0;i--) {
            NbtCompound stack=inventory.getCompound(i),tag=stack.getCompound("tag");
            if(payment.equals(tag.getString("TecniRitualPayment"))) {
                int count=stack.getByte("Count")&255;tag.remove("TecniRitualPayment");
                if(count<=1)inventory.remove(i);else stack.putByte("Count",(byte)(count-1));
                return true;
            }
        }
        return false;
    }
}
