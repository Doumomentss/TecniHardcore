package tecni.death.mixin;
import tecni.death.DeathOverlay;

import net.minecraft.class_757;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = class_757.class, remap = false)
public class RenderMixin {
    // Draw after HUD/screens have flushed but BEFORE vanilla pops the GUI
    // model-view (-11000 Z). At TAIL, z=0 is outside the GUI near/far planes,
    // so the animation plays but every pixel is clipped.
    @Inject(method = "method_3192", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/class_4587;method_22909()V",
        shift = At.Shift.BEFORE), remap = false)
    private void tecniOverlay(float tickDelta, long startTime, boolean tick, CallbackInfo ci) {
        DeathOverlay.render();
    }
}
