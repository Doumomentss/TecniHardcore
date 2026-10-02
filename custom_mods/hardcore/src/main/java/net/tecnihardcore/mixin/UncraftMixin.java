package net.tecnihardcore.mixin;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
@Pseudo
@Mixin(targets="net.pitan76.uncraftingtable.InsertSlot",remap=false)
public class UncraftMixin {
 @ModifyVariable(method="updateOutSlot",at=@At("HEAD"),argsOnly=true,remap=false)
 private ItemStack preventRelicUncrafting(ItemStack input) {
     return Registries.ITEM.getId(input.getItem()).getNamespace().equals("tecnihardcore")?ItemStack.EMPTY:input;
 }
}
