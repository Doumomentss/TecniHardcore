package net.tecnihardcore;

import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.block.*;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import java.util.*;

/** Reservoir of blocks actually removed; never spawns FallingBlockEntity. */
final class StormRubble {
    static final Identifier CHANNEL=Hardcore.id("storm_rubble_v1");
    private record Sample(BlockPos position,int state){}
    private static final class Batch {int seen;final List<Sample> samples=new ArrayList<>();}
    private static final Map<UUID,Batch> batches=new HashMap<>();
    static void removed(Cataclysms.Hazard h,BlockPos pos,BlockState state){
        Batch b=batches.computeIfAbsent(h.id,k->new Batch());int index=h.world.random.nextInt(++b.seen);
        Sample sample=new Sample(pos.toImmutable(),Block.getRawIdFromState(state));
        if(b.samples.size()<32)b.samples.add(sample);else if(index<32)b.samples.set(index,sample);
    }
    static void flush(Cataclysms.Hazard h){
        Batch batch=batches.remove(h.id);if(batch==null||batch.samples.isEmpty())return;
        for(var player:h.world.getPlayers(p->p.getPos().subtract(h.position()).horizontalLength()<h.radius+192))if(ServerPlayNetworking.canSend(player,CHANNEL)){
            var b=PacketByteBufs.create();b.writeUuid(h.id);b.writeVarInt(h.age);b.writeVarInt(batch.seen);b.writeVarInt(batch.samples.size());
            for(var s:batch.samples){b.writeBlockPos(s.position);b.writeVarInt(s.state);}ServerPlayNetworking.send(player,CHANNEL,b);
        }
    }
    static void forget(UUID id){batches.remove(id);}
    static void clear(){batches.clear();}
}
