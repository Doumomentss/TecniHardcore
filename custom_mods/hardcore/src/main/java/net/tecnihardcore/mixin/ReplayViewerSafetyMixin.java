package net.tecnihardcore.mixin;

import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.KeepAliveS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.replaymod.replaystudio.lib.viaversion.api.protocol.packet.State;

/** Recorded heartbeats belong to the original connection, never to the moderator. */
@Pseudo
@Mixin(targets="me.senseiwells.replay.viewer.ReplayViewer", remap=false)
public abstract class ReplayViewerSafetyMixin {
    // Decoded replay chunks never pass through AntiXray's live obfuscation job.
    // Its default false flag otherwise blocks the entire connection queue,
    // including new heartbeats. Scope this to operator replay playback only.
    @Inject(method="send$ServerReplay",at=@At("HEAD"),remap=false)
    private void tecni$readyRecordedChunk(Packet<?> packet, CallbackInfo info) {
        if(packet instanceof ChunkDataS2CPacket && net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("antixray")) {
            try{packet.getClass().getMethod("antixray$setReady",boolean.class).invoke(packet,true);}
            catch(ReflectiveOperationException error){throw new IllegalStateException("No se pudo preparar el chunk grabado para AntiXray",error);}
        }
    }

    // 1.20.1 has no CONFIGURATION phase; recorded game packets use PLAY.
    @Redirect(method="streamReplay",at=@At(value="FIELD",target="Lcom/replaymod/replaystudio/lib/viaversion/api/protocol/packet/State;CONFIGURATION:Lcom/replaymod/replaystudio/lib/viaversion/api/protocol/packet/State;"),remap=false)
    private State tecni$usePlayProtocol() {return State.PLAY;}

    @Inject(method="shouldSendPacket",at=@At("HEAD"),cancellable=true,remap=false)
    private void tecni$ignoreRecordedHeartbeat(Packet<?> packet, CallbackInfoReturnable<Boolean> result) {
        if(packet instanceof KeepAliveS2CPacket)result.setReturnValue(false);
    }
}
