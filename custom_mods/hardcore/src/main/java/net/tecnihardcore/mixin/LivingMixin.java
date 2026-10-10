package net.tecnihardcore.mixin;
import net.tecnihardcore.*;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(LivingEntity.class)
public class LivingMixin {
    @ModifyVariable(method="damage",at=@At("HEAD"),argsOnly=true,ordinal=0)
    private float tecniDistanceDamage(float amount,DamageSource source) {
        return DistanceDifficulty.damage(source,amount);
    }
    @Inject(method="tryUseTotem",at=@At("HEAD"),cancellable=true)
    private void relic(DamageSource source, CallbackInfoReturnable<Boolean> ci) {
        if ((Object)this instanceof ServerPlayerEntity p) {
            if (EventDirector.practice(p)) ci.setReturnValue(false);
            else if (Hardcore.blocked(p)) ci.setReturnValue(false);
            else if (Hardcore.useRelic(p,source)) ci.setReturnValue(true);
        }
    }
    @Inject(method="tryUseTotem",at=@At("RETURN"))
    private void vanillaCooldown(DamageSource source, CallbackInfoReturnable<Boolean> ci) {
        if (ci.getReturnValueZ() && (Object)this instanceof ServerPlayerEntity p && !Hardcore.blocked(p)) Hardcore.cooldown(p);
    }
}
