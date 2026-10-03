package net.tecnihardcore.mixin;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.tecnihardcore.ExpansionClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Minecraft's single-key lookup cannot own both jump and mount ascent. Mirror both. */
@Mixin(KeyBinding.class)
public abstract class MountKeyMixin {
    @Inject(method="setKeyPressed",at=@At("TAIL"))
    private static void sharedSpace(InputUtil.Key key,boolean pressed,CallbackInfo ci){
        var client=MinecraftClient.getInstance();
        if(client==null||client.options==null)return;
        if(KeyBindingHelper.getBoundKeyOf(client.options.jumpKey).equals(key))client.options.jumpKey.setPressed(pressed);
        ExpansionClient.mirrorAscent(key,pressed);
    }
}
