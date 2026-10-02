package net.tecnihardcore.mixin;
import net.tecnihardcore.AuthBootstrap;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.network.packet.c2s.play.CommandExecutionC2SPacket;
import net.minecraft.network.message.LastSeenMessageList;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=ServerPlayNetworkHandler.class,priority=1100)
public class ClaimMixin {
 @Shadow public ServerPlayerEntity player;
 @Inject(method="handleCommandExecution",at=@At("HEAD"),cancellable=true)
 private void claim(CommandExecutionC2SPacket packet,LastSeenMessageList messages,CallbackInfo ci){if(AuthBootstrap.intercept(player,packet.command()))ci.cancel();}
}
