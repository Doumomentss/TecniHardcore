package net.tecnihardcore.mixin;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ConnectScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ConnectScreen.class)
public abstract class ShaderConnectMixin {
 @Inject(method="connect(Lnet/minecraft/client/gui/screen/Screen;Lnet/minecraft/client/MinecraftClient;Lnet/minecraft/client/network/ServerAddress;Lnet/minecraft/client/network/ServerInfo;Z)V",at=@At("HEAD"))
 private static void warmup(Screen parent,MinecraftClient client,ServerAddress address,ServerInfo info,boolean quickPlay,CallbackInfo ci){net.tecnihardcore.ShaderWarmup.prepare();}
}
