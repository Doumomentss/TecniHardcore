package net.tecnihardcore;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.block.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.ParticlesMode;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.*;
import java.util.*;

/** Bounded client meshes of actual uprooted blocks; no physics entities or item drops. */
final class StormRubbleVisuals {
    private record Flying(Vec3d origin,BlockState state,int started,int seed){}
    private static final Map<UUID,Deque<Flying>> flying=new HashMap<>();
    private static final Map<UUID,BlockState[]> palette=new HashMap<>();
    private static final Map<UUID,Integer> received=new HashMap<>();
    static void init(){ClientPlayNetworking.registerGlobalReceiver(StormRubble.CHANNEL,(client,handler,b,response)->{
        UUID id=b.readUuid();int age=b.readVarInt(),removed=b.readVarInt(),count=b.readVarInt();
        if(age<200||age>12000||removed<1||removed>200000||count<1||count>32)return;
        var list=new ArrayList<Flying>();var materials=new ArrayList<BlockState>();
        for(int i=0;i<count;i++){BlockPos pos=b.readBlockPos();int raw=b.readVarInt();BlockState state=Block.getStateFromRawId(raw);if(state.isAir()||state.getBlock() instanceof FluidBlock)continue;list.add(new Flying(Vec3d.ofCenter(pos),state,age,age*37+i*919));if(materials.size()<8)materials.add(state);}
        client.execute(()->{if(client.world==null||age<=received.getOrDefault(id,-1))return;received.put(id,age);var queue=flying.computeIfAbsent(id,k->new ArrayDeque<>());for(var block:list){while(queue.size()>=1024)queue.removeFirst();queue.addLast(block);}if(!materials.isEmpty())palette.put(id,materials.toArray(BlockState[]::new));});
    });}
    static void forget(UUID id){flying.remove(id);palette.remove(id);received.remove(id);}
    static void clear(){flying.clear();palette.clear();received.clear();}
    static BlockState[] palette(UUID id,BlockState[] fallback){return palette.getOrDefault(id,fallback);}
    static void render(UUID id,MatrixStack m,VertexConsumerProvider vertices,Vec3d center,Vec3d eye,double time,int width,double height,boolean distant,ParticlesMode quality){
        var queue=flying.get(id);if(queue==null)return;queue.removeIf(v->time-v.started>160||time<v.started-20);
        int limit=distant?96:quality==ParticlesMode.MINIMAL?128:quality==ParticlesMode.DECREASED?384:768;
        int stride=Math.max(1,(queue.size()+limit-1)/limit),index=0;
        var renderer=MinecraftClient.getInstance().getBlockRenderManager();
        for(var block:queue){if(index++%stride!=0)continue;double age=Math.max(0,time-block.started),a=block.seed*.017+age*.13;
            double rise=Math.min(height*.88,age*(.7+(block.seed&7)*.08));
            double radius=Math.min(width*.36,18+rise*.45),eject=Math.max(0,age-100);
            Vec3d whirl=center.add(Math.cos(a)*(radius+eject*1.8),rise-eject*eject*.015,Math.sin(a)*(radius+eject*1.8));
            double capture=Math.min(1,age/24);Vec3d p=block.origin.lerp(whirl,capture);
            if(p.squaredDistanceTo(eye)>512*512)continue;
            m.push();m.translate(p.x,p.y,p.z);m.multiply(RotationAxis.POSITIVE_Y.rotation((float)(age*.19+block.seed)));m.multiply(RotationAxis.POSITIVE_Z.rotation((float)(age*.11)));m.translate(-.5,-.5,-.5);
            renderer.renderBlockAsEntity(block.state,m,vertices,15728880,OverlayTexture.DEFAULT_UV);m.pop();
        }
    }
    private StormRubbleVisuals(){}
}
