package net.tecnihardcore;
import net.fabricmc.fabric.api.client.networking.v1.*;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;
import java.util.*;

public final class RescueHud {
    private static final net.minecraft.client.render.VertexConsumerProvider.Immediate beamVertices=net.minecraft.client.render.VertexConsumerProvider.immediate(new net.minecraft.client.render.BufferBuilder(4096));
    private record Signal(UUID owner,String name,Vec3d pos,int seconds){}
    private static List<Signal> signals=List.of();private static long received;private static Object world;
    public static void init(){
        ClientPlayNetworking.registerGlobalReceiver(RescueBeacon.CHANNEL,(client,h,b,response)->{
            int count=b.readVarInt();if(count<0||count>16)return;List<Signal> next=new ArrayList<>();for(int i=0;i<count;i++)next.add(new Signal(b.readUuid(),b.readString(16),new Vec3d(b.readDouble(),b.readDouble(),b.readDouble()),b.readVarInt()));client.execute(()->{signals=List.copyOf(next);received=System.nanoTime();});
        });
        ClientPlayConnectionEvents.DISCONNECT.register((h,c)->signals=List.of());
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(c->{if(c.world!=world){signals=List.of();world=c.world;}if(c.player==null||c.world==null||System.nanoTime()-received>=3_000_000_000L)signals=List.of();});
        net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents.END.register(ctx->{
            var c=MinecraftClient.getInstance();if(c.player==null||c.world==null||signals.isEmpty()||c.options.hudHidden)return;
            c.getFramebuffer().beginWrite(false);var matrices=ctx.matrixStack();var eye=ctx.camera().getPos();matrices.push();matrices.translate(-eye.x,-eye.y,-eye.z);
            float start=com.mojang.blaze3d.systems.RenderSystem.getShaderFogStart(),end=com.mojang.blaze3d.systems.RenderSystem.getShaderFogEnd();com.mojang.blaze3d.systems.RenderSystem.setShaderFogStart(1024);com.mojang.blaze3d.systems.RenderSystem.setShaderFogEnd(2048);com.mojang.blaze3d.systems.RenderSystem.disableDepthTest();
            var lines=beamVertices.getBuffer(StormLayers.signal());
            for(var s:signals){Vec3d direction=s.pos.subtract(eye);boolean distant=direction.length()>96;Vec3d point=distant?eye.add(direction.normalize().multiply(96)):s.pos;Vec3d across=new Vec3d(-direction.z,0,direction.x).normalize();double bottom=distant?eye.y-40:point.y+1;float fade=(float)Math.min(1,Math.max(0,s.seconds-(System.nanoTime()-received)/1e9)/3);for(int i=0;i<7;i++){Vec3d offset=across.multiply((i-3)*.22);StormGeometry.beam(matrices,lines,new Vec3d(point.x+offset.x,bottom,point.z+offset.z),new Vec3d(point.x+offset.x,eye.y+320,point.z+offset.z),.08,i==3?.8F:.25F,i==3?1:.8F,1,fade*(i==3?1:.35F));}StormGeometry.ring(matrices,lines,point.add(0,2,0),3,.4F,.85F,1,fade);}
            beamVertices.draw();com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();com.mojang.blaze3d.systems.RenderSystem.setShaderFogStart(start);com.mojang.blaze3d.systems.RenderSystem.setShaderFogEnd(end);matrices.pop();
        });
        HudRenderCallback.EVENT.register((d,delta)->{
            var c=MinecraftClient.getInstance();if(c.player==null||c.options.hudHidden)return;
            int right=d.getScaledWindowWidth()-12,y=70;
            if(System.nanoTime()-received<3_000_000_000L)for(var s:signals){
                if(s.owner.equals(c.player.getUuid()))continue;Vec3d v=s.pos.subtract(c.player.getPos());double bearing=net.minecraft.util.math.MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(-v.x,v.z))-c.player.getYaw());String direction=Math.abs(bearing)<25?"DELANTE":Math.abs(bearing)>155?"DETRÁS":bearing>0?"DERECHA":"IZQUIERDA";
                String line=s.name+" · "+(int)v.length()+" m · "+direction;int x=right-c.textRenderer.getWidth(line);d.fill(x-4,y-3,right+4,y+21,0xaa071b24);d.drawTextWithShadow(c.textRenderer,line,x,y,0xff99d9ff);d.drawTextWithShadow(c.textRenderer,"Auxilio · "+s.seconds+" s",x,y+11,0xffdbc685);y+=29;
            }
            y=d.getScaledWindowHeight()-65;
            for(var stack:c.player.getInventory().armor)if(stack.isDamageable()&&(stack.getMaxDamage()-stack.getDamage())/(double)stack.getMaxDamage()<=.15){d.drawItem(stack,right-16,y);d.drawTextWithShadow(c.textRenderer,"¡Equipo por romperse!",right-24-c.textRenderer.getWidth("¡Equipo por romperse!"),y+5,0xffffaa66);y-=18;}
        });
    }
}
