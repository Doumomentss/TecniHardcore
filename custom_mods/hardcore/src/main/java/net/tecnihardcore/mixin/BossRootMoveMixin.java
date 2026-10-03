package net.tecnihardcore.mixin;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.tecnihardcore.BossRoots;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ServerPlayNetworkHandler.class)
public class BossRootMoveMixin {
 @Shadow public ServerPlayerEntity player;
 @Inject(method="onPlayerMove",at=@At("TAIL")) private void rootMove(PlayerMoveC2SPacket packet,CallbackInfo ci){BossRoots.enforce(player);}
}
