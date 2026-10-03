package net.tecnihardcore.mixin;
import net.tecnihardcore.BossRoots;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(LivingEntity.class)
public class BossRootMixin {
 @Inject(method="jump",at=@At("HEAD"),cancellable=true) private void rootJump(CallbackInfo ci){if(((LivingEntity)(Object)this).hasStatusEffect(BossRoots.ROOT))ci.cancel();}
}
