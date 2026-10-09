package net.tecnihardcore;
import net.fabricmc.fabric.api.client.networking.v1.*;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;

final class ModerationClient {
    private static long until;
    static void init(){
        ClientPlayNetworking.registerGlobalReceiver(Moderation.SYNC,(c,h,b,r)->{int seconds=b.readVarInt();if(seconds<0||seconds>1800)return;c.execute(()->until=System.nanoTime()+seconds*1_000_000_000L);});
        ClientPlayConnectionEvents.DISCONNECT.register((h,c)->until=0);
        HudRenderCallback.EVENT.register((draw,delta)->{var c=MinecraftClient.getInstance();if(c.player==null||c.options.hudHidden)return;long seconds=Math.max(0,(until-System.nanoTime()+999_999_999)/1_000_000_000);if(seconds>0)draw.drawTextWithShadow(c.textRenderer,String.format("Protección PvP: %d:%02d · mobs activos",seconds/60,seconds%60),12,83,0x85ddff);});
    }
}
