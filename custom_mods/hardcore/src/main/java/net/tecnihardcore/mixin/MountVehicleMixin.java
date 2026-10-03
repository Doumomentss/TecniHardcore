package net.tecnihardcore.mixin;

import net.minecraft.network.packet.c2s.play.VehicleMoveC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.tecnihardcore.CrystalMount;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** These vehicles accept bounded control inputs, never arbitrary client coordinates. */
@Mixin(ServerPlayNetworkHandler.class)
public abstract class MountVehicleMixin {
    @Shadow public ServerPlayerEntity player;
    @Inject(method="onVehicleMove",at=@At("HEAD"),cancellable=true)
    private void tecni$authoritativeFlight(VehicleMoveC2SPacket packet,CallbackInfo ci){
        if(player.getRootVehicle() instanceof CrystalMount)ci.cancel();
    }
}
