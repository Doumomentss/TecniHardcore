package net.tecnihardcore;
import net.minecraft.entity.attribute.*;
import net.minecraft.entity.effect.*;
import net.minecraft.registry.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import java.util.*;
public final class BossRoots {
    public static final StatusEffect ROOT=Registry.register(Registries.STATUS_EFFECT,Hardcore.id("rooted"),new Root());
    private static final Map<UUID,Vec3d> anchors=new HashMap<>();
    private static final class Root extends StatusEffect {Root(){super(StatusEffectCategory.HARMFUL,0xa778eb);addAttributeModifier(EntityAttributes.GENERIC_MOVEMENT_SPEED,"51b7025b-5b44-4ae9-8f96-bff192ebd5a0",-1,EntityAttributeModifier.Operation.MULTIPLY_TOTAL);}}
    public static void init(){net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(s->{for(var p:s.getPlayerManager().getPlayerList()){if(!p.hasStatusEffect(ROOT)||!p.isAlive()||p.isSpectator()||p.isCreative()){anchors.remove(p.getUuid());continue;}anchors.putIfAbsent(p.getUuid(),p.getPos());p.setVelocity(0,p.getVelocity().y,0);enforce(p);}});net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.register((h,s)->anchors.remove(h.player.getUuid()));net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(s->anchors.clear());}
    public static boolean apply(ServerPlayerEntity p){long now=System.currentTimeMillis();if(Hardcore.soul(p).rootImmuneUntil>now)return false;Hardcore.soul(p).rootImmuneUntil=now+12_000;Hardcore.souls.save();anchors.put(p.getUuid(),p.getPos());p.addStatusEffect(new StatusEffectInstance(ROOT,40));return true;}
    public static void enforce(ServerPlayerEntity p){var anchor=anchors.get(p.getUuid());if(anchor==null||!p.hasStatusEffect(ROOT))return;double dx=p.getX()-anchor.x,dz=p.getZ()-anchor.z;if(dx*dx+dz*dz>.0001)p.networkHandler.requestTeleport(anchor.x,p.getY(),anchor.z,p.getYaw(),p.getPitch());}
}
