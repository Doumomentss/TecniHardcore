package tecni.death.mixin;
import tecni.death.DeathOverlay;

import net.minecraft.class_634;
import net.minecraft.class_5892;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = class_634.class, remap = false)
public class DeathPacketMixin {
    // ClientPlayNetworkHandler.onDeathMessage: TAIL runs on the client thread
    // after vanilla handles immediate respawn. No dependency on remaining lives.
    @Inject(method = "method_34075", at = @At("TAIL"), remap = false)
    private void tecniDeath(class_5892 packet, CallbackInfo ci) {
        DeathOverlay.onDeath(packet.method_34144());
    }
}
