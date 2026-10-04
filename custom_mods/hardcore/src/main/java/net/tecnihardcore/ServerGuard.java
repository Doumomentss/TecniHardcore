package net.tecnihardcore;

import java.util.*;
import net.fabricmc.fabric.api.event.player.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Vec3d;

/** Native registry-aware abuse limits; unusual movement is diagnostic, never an automatic penalty. */
final class ServerGuard {
    static final class State {int window=-100,count,blocked,movement;long lastAlert;Vec3d previous;int dimension;}
    static final Map<UUID,State> states=new HashMap<>();
    static final Set<UUID> alerts=new HashSet<>();
    static void init(){
        AttackEntityCallback.EVENT.register((p,w,h,e,hit)->allow(p)?ActionResult.PASS:ActionResult.FAIL);
        UseBlockCallback.EVENT.register((p,w,h,hit)->allow(p)?ActionResult.PASS:ActionResult.FAIL);
        UseItemCallback.EVENT.register((p,w,h)->allow(p)?TypedActionResult.pass(p.getStackInHand(h)):TypedActionResult.fail(p.getStackInHand(h)));
        ServerPlayConnectionEvents.DISCONNECT.register((h,s)->s.execute(()->{states.remove(h.player.getUuid());alerts.remove(h.player.getUuid());}));
        ServerLifecycleEvents.SERVER_STOPPED.register(s->{states.clear();alerts.clear();});
        ServerTickEvents.END_SERVER_TICK.register(s->{if(s.getTicks()%20!=0)return;for(var p:s.getPlayerManager().getPlayerList()){
            var st=states.computeIfAbsent(p.getUuid(),k->new State());int dimension=p.getWorld().getRegistryKey().hashCode();
            if(st.previous!=null&&st.dimension==dimension&&!p.isCreative()&&!p.isSpectator()&&AuthBootstrap.authenticated(p)&&!special(p)){
                Vec3d delta=p.getPos().subtract(st.previous);
                // Threshold is deliberately above sprinting, boats and normal server movement.
                if(delta.horizontalLength()>48&&delta.horizontalLength()<96){st.movement++;notice(p,"Desplazamiento inusual: revisar contexto, sin sanción automática");}
            }
            st.previous=p.getPos();st.dimension=dimension;
        }});
    }
    static boolean special(ServerPlayerEntity p){
        if(p.hasVehicle()||p.isFallFlying()||p.hasStatusEffect(net.minecraft.entity.effect.StatusEffects.LEVITATION)||p.hasStatusEffect(net.minecraft.entity.effect.StatusEffects.SPEED)||p.hasStatusEffect(BossRoots.ROOT)||Rituals.participant(p.getUuid()))return true;
        for(var h:Cataclysms.active.values())if(h.world==p.getWorld()&&h.age>=200&&!h.stopping&&p.getPos().subtract(Vec3d.ofCenter(h.center)).horizontalLength()<h.radius+12)return true;
        return false;
    }
    static boolean allow(net.minecraft.entity.player.PlayerEntity player){
        if(!(player instanceof ServerPlayerEntity p))return true;
        if(!AuthBootstrap.authenticated(p))return false;
        if(p.isCreative()||p.isSpectator())return true;
        var st=states.computeIfAbsent(p.getUuid(),k->new State());int tick=p.getServer().getTicks();
        if(tick-st.window>=20){st.window=tick;st.count=0;}
        if(++st.count<=80)return true;
        st.blocked++;notice(p,"Exceso de ataques/interacciones: acciones adicionales bloqueadas");return false;
    }
    static void notice(ServerPlayerEntity p,String message){
        var st=states.get(p.getUuid());long now=System.currentTimeMillis();if(now-st.lastAlert<5000)return;st.lastAlert=now;
        Hardcore.LOG.warn("TECNI GUARD {}: {}",p.getUuidAsString(),message);
        for(UUID id:alerts){var op=p.getServer().getPlayerManager().getPlayer(id);if(op!=null&&op.hasPermissionLevel(4)&&AuthBootstrap.authenticated(op))Social.say(op,"[TecniGuard] "+p.getName().getString()+": "+message);}
    }
    static String summary(UUID id){var s=states.get(id);return s==null?"Sin acciones registradas en esta conexión":"Acciones bloqueadas: "+s.blocked+" · desplazamientos a revisar: "+s.movement;}
}
