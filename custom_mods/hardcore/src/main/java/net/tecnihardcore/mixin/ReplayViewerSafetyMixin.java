package net.tecnihardcore.mixin;

import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.KeepAliveS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.replaymod.replaystudio.lib.viaversion.api.protocol.packet.State;

/** Recorded heartbeats belong to the original connection, never to the moderator. */
@Pseudo
@Mixin(targets="me.senseiwells.replay.viewer.ReplayViewer", remap=false)
public abstract class ReplayViewerSafetyMixin {
    // 1.20.1 has LOGIN -> PLAY, with no CONFIGURATION phase (introduced in 1.20.2).
    @Redirect(method="streamReplay",at=@At(value="FIELD",target="Lcom/replaymod/replaystudio/lib/viaversion/api/protocol/packet/State;CONFIGURATION:Lcom/replaymod/replaystudio/lib/viaversion/api/protocol/packet/State;"),remap=false)
    private State tecni$startAtLoginProtocol() {return State.LOGIN;}

    @Inject(method="shouldSendPacket",at=@At("HEAD"),cancellable=true,remap=false)
    private void tecni$ignoreRecordedHeartbeat(Packet<?> packet, CallbackInfoReturnable<Boolean> result) {
        if(packet instanceof KeepAliveS2CPacket)result.setReturnValue(false);
    }
}
