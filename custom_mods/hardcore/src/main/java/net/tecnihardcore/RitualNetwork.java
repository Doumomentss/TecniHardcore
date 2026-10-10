package net.tecnihardcore;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import java.util.*;
public final class RitualNetwork {
    public static final Identifier HELLO=Hardcore.id("pack_v15");
    public static final Identifier SOUL=Hardcore.id("soul_v3"), STATE=Hardcore.id("ritual_v3"), OPEN=Hardcore.id("altar_open_v3"), SELECT=Hardcore.id("altar_select_v3"), QUERY=Hardcore.id("altar_query_v3"), RELIC=Hardcore.id("relic_v2");
    private record Offer(BlockPos altar,UUID nonce,long expires){}
    private static final Map<UUID,Offer> offers=new HashMap<>();
    public static void init() {
        ServerLoginConnectionEvents.QUERY_START.register((h,s,sender,sync)->{var b=PacketByteBufs.create();b.writeVarInt(14);sender.sendPacket(HELLO,b);});
        ServerLoginNetworking.registerGlobalReceiver(HELLO,(s,h,understood,b,sync,sender)->{
            boolean valid=false;try{valid=understood&&b!=null&&b.isReadable()&&b.readVarInt()==14;}catch(Exception ignored){}
            if(!valid)h.disconnect(Text.literal("Actualiza a TecniHardcore 2.9.1. Cierra Minecraft y abre el launcher actualizado para entrar."));
        });
        ServerPlayNetworking.registerGlobalReceiver(SELECT,(s,p,h,b,r)->{
            BlockPos altar=b.readBlockPos();UUID nonce=b.readUuid(),target=b.readUuid();s.execute(()->{
                Offer offer=offers.remove(p.getUuid());
                if(offer==null||!offer.nonce.equals(nonce)||!offer.altar.equals(altar)||offer.expires<s.getTicks()) {p.sendMessage(Text.literal("La selección caducó. Vuelve a abrir el santuario."),false);return;}
                var other=s.getPlayerManager().getPlayer(target);if(other!=null)Rituals.beginAt(p,other,altar);
            });
        });
        ServerPlayNetworking.registerGlobalReceiver(QUERY,(s,p,h,b,r)->{BlockPos altar=b.readBlockPos();s.execute(()->open(p,altar));});
    }
    public static boolean compatible(ServerPlayerEntity p) {return ServerPlayNetworking.canSend(p,SOUL)&&ServerPlayNetworking.canSend(p,STATE)&&ServerPlayNetworking.canSend(p,OPEN);}
    public static void forget(UUID id){offers.remove(id);}
    public static void open(ServerPlayerEntity p,BlockPos altar) {
        if(!AuthBootstrap.authenticated(p)||!Rituals.near(p,altar)||!p.getWorld().getBlockState(altar).isOf(Sanctuaries.CORE))return;
        if(!compatible(p)){p.sendMessage(Text.literal("Actualiza tu launcher al paquete TecniHardcore 2.9.1 para usar el santuario."),false);return;}
        Offer previous=offers.get(p.getUuid());if(previous!=null&&previous.expires-p.getServer().getTicks()>190)return;
        UUID nonce=UUID.randomUUID();offers.put(p.getUuid(),new Offer(altar,nonce,p.getServer().getTicks()+200));
        String problem=Rituals.casterProblem(p,altar);
        var candidates=p.getServer().getPlayerManager().getPlayerList().stream().filter(t->t!=p&&t.getWorld()==p.getWorld()&&Rituals.near(t,altar)&&AuthBootstrap.authenticated(t)&&Hardcore.soul(t).lives==0&&t.isSpectator()).toList();
        var b=PacketByteBufs.create();b.writeBlockPos(altar);b.writeUuid(nonce);b.writeString(problem==null?"":problem);b.writeVarInt(candidates.size());
        for(var t:candidates){b.writeUuid(t.getUuid());b.writeString(t.getName().getString());b.writeVarInt(Hardcore.soul(t).resurrections);}
        ServerPlayNetworking.send(p,OPEN,b);
    }
    public static void visual(ServerPlayerEntity viewer,UUID ritual,BlockPos altar,UUID caster,UUID target,net.minecraft.util.math.Vec3d anchor,net.minecraft.util.math.Vec3d landing,int elapsed,int state,String name) {
        if(!ServerPlayNetworking.canSend(viewer,STATE))return;
        var b=PacketByteBufs.create();b.writeUuid(ritual);b.writeBlockPos(altar);b.writeUuid(caster);b.writeUuid(target);
        b.writeDouble(anchor.x);b.writeDouble(anchor.y);b.writeDouble(anchor.z);b.writeDouble(landing.x);b.writeDouble(landing.y);b.writeDouble(landing.z);
        b.writeVarInt(elapsed);b.writeByte(state);b.writeString(name);ServerPlayNetworking.send(viewer,STATE,b);
    }
    public static void relic(ServerPlayerEntity p,int type){
        for(var v:p.getServerWorld().getPlayers())if(v.squaredDistanceTo(p)<4096&&ServerPlayNetworking.canSend(v,RELIC)){
            var b=PacketByteBufs.create();b.writeInt(p.getId());b.writeByte(type);ServerPlayNetworking.send(v,RELIC,b);
        }
    }
}
