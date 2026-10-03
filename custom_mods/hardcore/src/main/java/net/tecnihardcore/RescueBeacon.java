package net.tecnihardcore;

import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.item.*;
import net.minecraft.registry.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.*;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import java.util.*;

/** Cooperative navigation only: no healing, teleportation, lives or loot. */
public final class RescueBeacon extends Item {
    public static final Identifier CHANNEL=Hardcore.id("rescue_v1");
    public static final Item ITEM=Registry.register(Registries.ITEM,Hardcore.id("baliza_auxilio"),new RescueBeacon());
    private record Signal(UUID owner,String name,World world,Vec3d position,long end){}
    private static final Map<UUID,Signal> active=new LinkedHashMap<>();
    private static int ticks;
    public RescueBeacon(){super(new Settings().maxDamage(16).fireproof().rarity(Rarity.RARE));}
    public static void init(){
        ServerLifecycleEvents.SERVER_STOPPING.register(s->active.clear());
        ServerTickEvents.END_SERVER_TICK.register(s->{if(++ticks%20!=0||Hardcore.souls==null)return;long now=System.currentTimeMillis();active.values().removeIf(v->v.end<=now||s.getPlayerManager().getPlayer(v.owner)==null);for(var p:s.getPlayerManager().getPlayerList())send(p,now);for(var v:active.values())if(v.world instanceof net.minecraft.server.world.ServerWorld w){for(int i=0;i<4;i++)w.spawnParticles(SanctuaryEffects.RUNE,v.position.x,v.position.y+1+i*1.4,v.position.z,2,.08,.1,.08,.01);}});
    }
    private static void send(ServerPlayerEntity p,long now){
        if(!AuthBootstrap.authenticated(p)||!ServerPlayNetworking.canSend(p,CHANNEL))return;
        var signals=active.values().stream().filter(v->v.world==p.getWorld()&&p.squaredDistanceTo(v.position)<=256*256).limit(16).toList();
        var b=PacketByteBufs.create();b.writeVarInt(signals.size());
        for(var v:signals){b.writeUuid(v.owner);b.writeString(v.name,16);b.writeDouble(v.position.x);b.writeDouble(v.position.y);b.writeDouble(v.position.z);b.writeVarInt((int)Math.max(0,(v.end-now)/1000));}ServerPlayNetworking.send(p,CHANNEL,b);
    }
    @Override public TypedActionResult<ItemStack> use(World world,net.minecraft.entity.player.PlayerEntity player,Hand hand){
        var stack=player.getStackInHand(hand);if(world.isClient)return TypedActionResult.success(stack);
        if(!(player instanceof ServerPlayerEntity p)||!AuthBootstrap.authenticated(p))return TypedActionResult.fail(stack);
        if(Hardcore.soul(p).lives==0||p.isSpectator()){p.sendMessage(Text.literal("La baliza requiere un superviviente con vidas."),true);return TypedActionResult.fail(stack);}
        long now=System.currentTimeMillis(),remaining=Hardcore.soul(p).rescueUsedAt+600_000-now;
        if(remaining>0){p.sendMessage(Text.literal("Baliza: disponible en "+((remaining+999)/1000)+" s."),true);return TypedActionResult.fail(stack);}
        Hardcore.soul(p).rescueUsedAt=now;Hardcore.souls.save();
        stack.damage(1,p,e->e.sendToolBreakStatus(hand));active.put(p.getUuid(),new Signal(p.getUuid(),p.getGameProfile().getName(),world,p.getPos(),now+60_000));
        world.playSound(null,p.getBlockPos(),SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME,SoundCategory.PLAYERS,1,.7F);
        for(var other:p.getServer().getPlayerManager().getPlayerList())if(other.getWorld()==world&&AuthBootstrap.authenticated(other)&&other.squaredDistanceTo(p)<=256*256){other.sendMessage(Text.literal("Auxilio: "+p.getGameProfile().getName()+" solicita compañía. La señal dura 60 segundos."),false);send(other,now);}
        return TypedActionResult.success(stack);
    }
    @Override public void appendTooltip(ItemStack stack,World world,List<Text> lines,net.minecraft.client.item.TooltipContext context){
        lines.add(Text.literal("Señala tu ubicación durante 60 s a 256 bloques."));
        lines.add(Text.literal("16 usos · enfriamiento compartido de 10 min."));
        lines.add(Text.literal("No recupera vidas ni transporta jugadores."));
    }
}
