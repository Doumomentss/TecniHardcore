package net.tecnihardcore.mixin;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.tecnihardcore.EventDirector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** All inventory-drop paths (Q, outside clicks and drag/drop), including foreign mod gear. */
@Mixin(PlayerEntity.class)
public abstract class EventIsolationMixin {
    @Inject(method="dropItem(Lnet/minecraft/item/ItemStack;ZZ)Lnet/minecraft/entity/ItemEntity;",at=@At("HEAD"),cancellable=true)
    private void trialDrop(ItemStack stack,boolean thrown,boolean retainOwner,CallbackInfoReturnable<ItemEntity> ci){if(EventDirector.temporary((PlayerEntity)(Object)this))ci.setReturnValue(null);}
}
