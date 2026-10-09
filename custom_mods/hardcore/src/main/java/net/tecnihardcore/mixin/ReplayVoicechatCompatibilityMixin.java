package net.tecnihardcore.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * ServerReplay 1.2.2 bundles an old Simple Voice Chat adapter.  Its player
 * state callback calls a constructor removed from current voicechat builds,
 * which breaks death recording even when voice recording is disabled.  Voice
 * audio is deliberately not part of Tecni moderation evidence, so skip that
 * optional event registration and keep the normal recorder independent.
 */
@Pseudo
@Mixin(targets = "me.senseiwells.replay.compat.voicechat.ReplayVoicechatPlugin", remap = false)
public abstract class ReplayVoicechatCompatibilityMixin {
    @Inject(method = "initialize", at = @At("HEAD"), cancellable = true, remap = false)
    private void tecni$skipReplayPluginRegistration(CallbackInfo info) {
        info.cancel();
    }

    @Inject(method = "registerEvents", at = @At("HEAD"), cancellable = true, remap = false)
    private void tecni$disableOutdatedVoiceAdapter(CallbackInfo info) {
        info.cancel();
    }
}
