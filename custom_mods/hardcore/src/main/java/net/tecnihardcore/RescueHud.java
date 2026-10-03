package net.tecnihardcore;
import net.fabricmc.fabric.api.client.networking.v1.*;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;
import java.util.*;

public final class RescueHud {
    private record Signal(UUID owner,String name,Vec3d pos,int seconds){}
    private static List<Signal> signals=List.of();private static long received;
    public static void init(){
        ClientPlayNetworking.registerGlobalReceiver(RescueBeacon.CHANNEL,(client,h,b,response)->{
            int count=b.readVarInt();if(count<0||count>16)return;List<Signal> next=new ArrayList<>();for(int i=0;i<count;i++)next.add(new Signal(b.readUuid(),b.readString(16),new Vec3d(b.readDouble(),b.readDouble(),b.readDouble()),b.readVarInt()));client.execute(()->{signals=List.copyOf(next);received=System.nanoTime();});
        });
        ClientPlayConnectionEvents.DISCONNECT.register((h,c)->signals=List.of());
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
