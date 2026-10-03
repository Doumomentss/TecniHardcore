package net.tecnihardcore.mixin;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.network.packet.c2s.play.CustomPayloadC2SPacket;
import net.tecnihardcore.AuthBootstrap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Cosmetic skin requests must respect the same authentication gate as commands. */
@Mixin(ServerPlayNetworkHandler.class)
public abstract class SkinAuthMixin {
 @Shadow public ServerPlayerEntity player;
 @Inject(method="onCustomPayload",at=@At("HEAD"),cancellable=true)
 private void authenticatedSkin(CustomPayloadC2SPacket packet,CallbackInfo ci){if(packet.getChannel().getNamespace().equals("fabrictailor")&&!AuthBootstrap.authenticated(player))ci.cancel();}
}
