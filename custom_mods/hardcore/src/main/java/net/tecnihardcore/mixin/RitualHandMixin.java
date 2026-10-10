package net.tecnihardcore.mixin;
import net.minecraft.client.render.GameRenderer;
import net.tecnihardcore.RitualCinema;
import net.tecnihardcore.IntroCinema;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(GameRenderer.class)
public abstract class RitualHandMixin {
    @Inject(method="renderHand",at=@At("HEAD"),cancellable=true) private void cinematic(CallbackInfo ci){if(IntroCinema.active()||RitualCinema.current()!=null)ci.cancel();}
}
