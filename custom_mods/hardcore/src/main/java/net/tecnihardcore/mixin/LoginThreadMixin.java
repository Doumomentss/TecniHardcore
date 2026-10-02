package net.tecnihardcore.mixin;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientLoginNetworkHandler;
import net.minecraft.network.NetworkThreadUtils;
import net.minecraft.network.packet.s2c.login.LoginQueryRequestS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Finish installing the connection screen before Fabric resolves login receivers. */
@Mixin(value=ClientLoginNetworkHandler.class,priority=1100)
public class LoginThreadMixin {
    @Inject(method="onQueryRequest",at=@At("HEAD"))
    private void loginOnClientThread(LoginQueryRequestS2CPacket packet,CallbackInfo ci){
        NetworkThreadUtils.forceMainThread(packet,(ClientLoginNetworkHandler)(Object)this,MinecraftClient.getInstance());
    }
}
