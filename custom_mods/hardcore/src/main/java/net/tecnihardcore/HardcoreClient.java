package net.tecnihardcore;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.*;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.item.ItemStack;
import com.mojang.blaze3d.systems.RenderSystem;

public final class HardcoreClient implements ClientModInitializer {
    private static int lives=-1;
    private static int resurrections;
    private static long ready;
    private static boolean arena;
    public void onInitializeClient() {
        ClientLoginNetworking.registerGlobalReceiver(RitualNetwork.HELLO,(c,h,b,listener)->{int protocol=b.readVarInt();var response=net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create();response.writeVarInt(protocol==10?10:0);return java.util.concurrent.CompletableFuture.completedFuture(response);});
        net.minecraft.client.render.block.entity.BlockEntityRendererFactories.register(Sanctuaries.ENTITY,SanctuaryRenderer::new);
        RitualVisuals.init(); BossVisuals.init(); RescueHud.init(); ExpansionClient.init();
        net.minecraft.client.render.block.entity.BlockEntityRendererFactories.register(TotemBoard.ENTITY,TotemBoardRenderer::new);
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(TrialBoss.TYPE,TrialBossRenderer::new);
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(TrialShard.TYPE,TrialBossRenderer.ShardRenderer::new);
        ClientPlayNetworking.registerGlobalReceiver(TotemBoard.OPEN,(client,h,b,response)->client.execute(()->client.setScreen(new TotemGuideScreen())));
        ClientPlayNetworking.registerGlobalReceiver(RitualNetwork.SOUL,(client,handler,buf,response)-> {
            int n=buf.readInt(); int r=buf.readVarInt(); long remaining=buf.readLong(); boolean zone=buf.readBoolean();
            client.execute(()->{lives=n;resurrections=r;arena=zone;ready=System.nanoTime()+remaining*1_000_000;});
        });
        ClientPlayNetworking.registerGlobalReceiver(Hardcore.ACTIVATE,(client,handler,buf,response)->{
            ItemStack item=buf.readItemStack(); client.execute(()->client.gameRenderer.showFloatingItem(item));
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler,client)->{lives=-1;ready=0;});
        HudRenderCallback.EVENT.register((draw,delta)->{
            MinecraftClient c=MinecraftClient.getInstance(); if(lives<0||c.player==null||c.options.hudHidden)return;
            int x=12,y=12; RenderSystem.enableBlend();
            for(int i=0;i<3;i++) {
                draw.setShaderColor(i<lives?1:.25F,i<lives?1:.25F,i<lives?1:.25F,1);
                draw.drawTexture(new Identifier("tecnihardcore","textures/gui/heart.png"),x+i*22,y,0,0,20,20,20,20);
            }
            draw.setShaderColor(1,1,1,1);
            draw.drawTextWithShadow(c.textRenderer,lives==0?"ELIMINADO":lives+" / 3 vidas",x,y+23,lives==1?0xff7755:0xf4d59b);
            long seconds=Math.max(0,(ready-System.nanoTime()+999_999_999)/1_000_000_000);
            draw.drawTextWithShadow(c.textRenderer,seconds>0?String.format("Tótems: %d:%02d",seconds/60,seconds%60):"Tótems preparados",x,y+35,seconds>0?0xf29d74:0x8cdbb5);
            draw.drawTextWithShadow(c.textRenderer,lives==0?"Ritual disponible · Resurrecciones: "+resurrections:"Resurrecciones: "+resurrections,x,y+47,0xb1b7c7);
            if(arena)draw.drawTextWithShadow(c.textRenderer,"Zona del jefe: 1 minuto",x,y+59,0xbda5ff);
            var ritual=RitualVisuals.nearest();if(ritual!=null){
                double elapsed=ritual.elapsed(delta);int w=Math.min(220,draw.getScaledWindowWidth()-24),left=(draw.getScaledWindowWidth()-w)/2,top=draw.getScaledWindowHeight()-65;
                draw.fill(left-4,top-15,left+w+4,top+12,0xaa07191a);
                String stage=ritual.state==1?"ALMA RESTAURADA":elapsed<80?"DESPERTAR":elapsed<280?"EXTRACCIÓN DEL ALMA":elapsed<520?"RECONSTRUCCIÓN":"RESURRECCIÓN";
                draw.drawCenteredTextWithShadow(c.textRenderer,stage+" · "+ritual.name,draw.getScaledWindowWidth()/2,top-11,0xa4f7cd);
                draw.fill(left,top+3,left+w,top+7,0xff263e3d);draw.fill(left,top+3,left+(int)(w*Math.min(1,elapsed/600)),top+7,0xff64d9ae);
            }
            if(RitualVisuals.heartFlash>0){float alpha=RitualVisuals.heartFlash/60F;draw.setShaderColor(1,1,1,alpha);int cx=draw.getScaledWindowWidth()/2,cy=draw.getScaledWindowHeight()/2;draw.getMatrices().push();draw.getMatrices().translate(cx-16,cy-16,0);draw.getMatrices().scale(1.6F,1.6F,1);draw.drawTexture(Hardcore.id("textures/gui/heart.png"),0,0,0,0,20,20,20,20);draw.getMatrices().pop();draw.setShaderColor(1,1,1,1);}
            RenderSystem.disableBlend();
        });
        ItemTooltipCallback.EVENT.register((stack,context,lines)->{
            if(stack.hasNbt()&&stack.getNbt().getBoolean("SacredHeart")) lines.add(Text.literal("Reliquia antigua: no recupera vidas. Consulta /tecni guia."));
            if(Hardcore.isRelic(stack.getItem())) {
                lines.add(Text.literal("Salva desde cualquier mano. Deja 2 corazones."));
                lines.add(Text.literal("Enfriamiento: 5 min; zona del jefe: 1 min. No salva del vacío."));
            }
        });
    }
}
