package net.tecnihardcore;
import net.minecraft.nbt.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class RitualPaymentTest {
 private NbtList inventory(int count){var list=new NbtList();var item=new NbtCompound();item.putString("id","tecnihardcore:corazon_sagrado");item.putByte("Count",(byte)count);var tag=new NbtCompound();tag.putString("TecniRitualPayment","test-payment");tag.putInt("CustomModelData",9);item.put("tag",tag);list.add(item);return list;}
 @Test void stackedOfferingLosesOneAndKeepsOtherData(){var list=inventory(8);assertTrue(RitualPayment.recover(list,"test-payment"));assertEquals(7,list.getCompound(0).getByte("Count"));assertEquals(9,list.getCompound(0).getCompound("tag").getInt("CustomModelData"));assertFalse(RitualPayment.recover(list,"test-payment"));assertEquals(7,list.getCompound(0).getByte("Count"));}
 @Test void singleOfferingIsRemoved(){var list=inventory(1);assertTrue(RitualPayment.recover(list,"test-payment"));assertEquals(0,list.size());}
 @Test void unrelatedPaymentDoesNotDebit(){var list=inventory(8);assertFalse(RitualPayment.recover(list,"other"));assertEquals(8,list.getCompound(0).getByte("Count"));}
}
