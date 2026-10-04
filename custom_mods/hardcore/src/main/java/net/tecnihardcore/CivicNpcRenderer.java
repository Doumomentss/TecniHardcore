package net.tecnihardcore;
import net.fabricmc.fabric.api.client.networking.v1.*;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.*;
import net.minecraft.client.render.entity.*;
import net.minecraft.client.render.entity.model.*;
import net.minecraft.client.texture.*;
import net.minecraft.util.Identifier;
import java.util.*;

public final class CivicNpcRenderer extends LivingEntityRenderer<CivicNpcEntity,PlayerEntityModel<CivicNpcEntity>> {
    private final PlayerEntityModel<CivicNpcEntity> wide,slim;
    private static final Map<String,Identifier> skins=new LinkedHashMap<>();private static final Map<String,Long> requested=new HashMap<>();
    public CivicNpcRenderer(EntityRendererFactory.Context ctx){super(ctx,new PlayerEntityModel<>(ctx.getPart(EntityModelLayers.PLAYER),false),.35F);wide=model;slim=new PlayerEntityModel<>(ctx.getPart(EntityModelLayers.PLAYER_SLIM),true);}
    @Override public void render(CivicNpcEntity entity,float yaw,float delta,net.minecraft.client.util.math.MatrixStack matrices,net.minecraft.client.render.VertexConsumerProvider vertices,int light){model=entity.slim()?slim:wide;super.render(entity,yaw,delta,matrices,vertices,light);}
    @Override public Identifier getTexture(CivicNpcEntity entity){String hash=entity.skin();if(!hash.isEmpty()){var skin=skins.get(hash);if(skin!=null)return skin;long now=System.currentTimeMillis();if(now-requested.getOrDefault(hash,0L)>4000&&ClientPlayNetworking.canSend(NpcSkins.REQUEST)){requested.put(hash,now);var b=PacketByteBufs.create();b.writeString(hash);ClientPlayNetworking.send(NpcSkins.REQUEST,b);}}String fallback=Set.of("steve","alex","efe","zuri").contains(entity.fallback())?entity.fallback():"steve";return new Identifier("minecraft","textures/entity/player/"+(entity.slim()?"slim":"wide")+"/"+fallback+".png");}
    static void init(){ClientPlayNetworking.registerGlobalReceiver(NpcSkins.DATA,(c,h,b,r)->{String hash=b.readString(64);byte[] png=b.readByteArray(65536);if(!hash.matches("[a-f0-9]{64}"))return;c.execute(()->{try{if(!java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(png)).equals(hash))return;var image=NativeImage.read(png);if(image.getWidth()!=64||image.getHeight()!=64){image.close();return;}if(skins.containsKey(hash)){image.close();return;}if(skins.size()>=128){var oldest=skins.keySet().iterator().next();c.getTextureManager().destroyTexture(skins.remove(oldest));}skins.put(hash,c.getTextureManager().registerDynamicTexture("tecni_npc_"+hash,new NativeImageBackedTexture(image)));}catch(Exception e){Hardcore.LOG.warn("NPC skin rejected: {}",e.getClass().getSimpleName());}});});ClientPlayConnectionEvents.DISCONNECT.register((h,c)->{for(var skin:skins.values())c.getTextureManager().destroyTexture(skin);skins.clear();requested.clear();});}
}
