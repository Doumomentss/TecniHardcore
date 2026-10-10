package net.tecnihardcore.mixin;
import net.minecraft.client.Mouse;
import net.tecnihardcore.RitualCinema;
import net.tecnihardcore.IntroCinema;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Mouse.class)
public abstract class RitualMouseMixin {
    @Shadow private double cursorDeltaX,cursorDeltaY;
    @Inject(method="updateMouse",at=@At("HEAD"),cancellable=true) private void cinematic(CallbackInfo ci){if(IntroCinema.active()||RitualCinema.current()!=null){cursorDeltaX=0;cursorDeltaY=0;ci.cancel();}}
}
