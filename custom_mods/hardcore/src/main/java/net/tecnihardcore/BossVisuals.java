package net.tecnihardcore;
import java.util.*;
import net.fabricmc.fabric.api.client.networking.v1.*;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
/** Solid geometric telegraphs remain visible with minimal particles. */
public final class BossVisuals {
 private record Cast(UUID id,int type,int age,int warn,int finish,long received,Vec3d origin,double aim,List<Vec3d> marks){}
 private static final Map<Integer,Cast> casts=new HashMap<>();
 private static Vec3d vec(net.minecraft.network.PacketByteBuf b){return new Vec3d(b.readDouble(),b.readDouble(),b.readDouble());}
 public static void init(){
  ClientPlayNetworking.registerGlobalReceiver(BossNetwork.FX,(c,h,b,r)->{int entity=b.readInt();UUID id=b.readUuid();int type=b.readVarInt(),age=b.readVarInt(),warn=b.readVarInt(),finish=b.readVarInt(),state=b.readUnsignedByte();Vec3d origin=vec(b);double aim=b.readDouble();int count=b.readVarInt();if(count<0||count>8)return;List<Vec3d> marks=new ArrayList<>();for(int i=0;i<count;i++)marks.add(vec(b));c.execute(()->{if(state==0)casts.put(entity,new Cast(id,type,age,warn,finish,System.nanoTime(),origin,aim,marks));else if(casts.containsKey(entity)&&casts.get(entity).id.equals(id))casts.remove(entity);});});
  ClientPlayConnectionEvents.DISCONNECT.register((h,c)->casts.clear());
  WorldRenderEvents.AFTER_ENTITIES.register(ctx->{var c=MinecraftClient.getInstance();if(c.world==null||ctx.consumers()==null)return;var ms=ctx.matrixStack();var eye=ctx.camera().getPos();var v=ctx.consumers().getBuffer(RenderLayer.getLines());ms.push();ms.translate(-eye.x,-eye.y,-eye.z);
   casts.entrySet().removeIf(e->c.world.getEntityById(e.getKey())==null||System.nanoTime()-e.getValue().received>4_000_000_000L);
   for(var cast:casts.values()){double age=cast.age+(System.nanoTime()-cast.received)/50_000_000.0;if(age>cast.finish||eye.squaredDistanceTo(cast.origin)>64*64)continue;boolean warning=age<cast.warn;float red=warning?1:.3F,green=warning?.6F:.8F;double elapsed=age-cast.warn;
    switch(cast.type){
     case 1,8->{circle(ms,v,cast.origin,warning?14:Math.min(14,elapsed*.8),red,green);if(cast.type==8&&elapsed>=12)circle(ms,v,cast.origin,Math.min(14,(elapsed-12)*.8),red,green);}
     case 2,7->line(ms,v,cast.origin,cast.aim,0,cast.type==7?16:17,red,green);
     case 4->{for(int i=-1;i<=1;i++)line(ms,v,cast.origin,cast.aim+i*Math.toRadians(25),0,18,red,green);}
     case 11->{for(var m:cast.marks)for(int i=0;i<4;i++)line(ms,v,m,i*Math.PI/4,-7,7,red,green);}
     default->{for(var m:cast.marks)circle(ms,v,m,cast.type==12?2.5:2,red,green);}
    }
   }ms.pop();
  });
 }
 private static void segment(MatrixStack m,VertexConsumer v,Vec3d a,Vec3d b,float r,float g){WorldRenderer.drawBox(m,v,Math.min(a.x,b.x)-.035,Math.min(a.y,b.y)+.07,Math.min(a.z,b.z)-.035,Math.max(a.x,b.x)+.035,Math.max(a.y,b.y)+.10,Math.max(a.z,b.z)+.035,r,g,1,1);}
 private static void circle(MatrixStack m,VertexConsumer v,Vec3d p,double radius,float r,float g){for(int i=0;i<40;i++){double a=i*Math.PI/20,b=(i+1)*Math.PI/20;segment(m,v,p.add(Math.cos(a)*radius,0,Math.sin(a)*radius),p.add(Math.cos(b)*radius,0,Math.sin(b)*radius),r,g);}}
 private static void line(MatrixStack m,VertexConsumer v,Vec3d p,double angle,int from,int to,float r,float g){for(int i=from;i<to;i++)segment(m,v,p.add(Math.cos(angle)*i,0,Math.sin(angle)*i),p.add(Math.cos(angle)*(i+1),0,Math.sin(angle)*(i+1)),r,g);}
}
