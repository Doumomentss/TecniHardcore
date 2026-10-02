package net.tecnihardcore.mixin;
import net.tecnihardcore.Hardcore;
import com.google.gson.*;
import net.minecraft.network.packet.s2c.query.QueryResponseS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.ServerMetadata;
import com.mojang.serialization.JsonOps;
@Mixin(QueryResponseS2CPacket.class)
public class StatusMixin {
    @Inject(method="write",at=@At("HEAD"),cancellable=true)
    private void tecniStatus(PacketByteBuf buffer,CallbackInfo ci) {
        ServerMetadata metadata=((QueryResponseS2CPacket)(Object)this).metadata();
        JsonObject root=ServerMetadata.CODEC.encodeStart(JsonOps.INSTANCE,metadata).result().orElseThrow().getAsJsonObject();
        root.add("tecnihardcore",JsonParser.parseString(Hardcore.statusJson));
        buffer.writeString(root.toString()); ci.cancel();
    }
}
