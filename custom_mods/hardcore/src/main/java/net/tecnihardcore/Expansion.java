package net.tecnihardcore;

import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
public final class Expansion {
    public static ExpansionStore store;
    private static final java.util.Map<String,Integer> absent=new java.util.HashMap<>();
    public static void init(){
        CrystalMount.init();Rewards.init();Cataclysms.init();
        ServerLifecycleEvents.SERVER_STARTED.register(s->{store=new ExpansionStore(s);absent.clear();boolean changed=false;for(var m:store.data.mounts.values())if(m.temporary&&m.state.equals("activa")){m.state="muerta";m.entity="";m.health=0;changed=true;}if(changed)store.save();});
        ServerEntityEvents.ENTITY_LOAD.register((e,w)->{if(e instanceof CrystalMount m&&store!=null){var entry=store.data.mounts.get(m.registryId());if(entry!=null&&entry.state.equals("activa")&&entry.entity.equals(m.getUuidAsString())){m.setHealth(entry.health);absent.remove(m.registryId());}}});
        ServerTickEvents.END_SERVER_TICK.register(s->{if(store==null||s.getTicks()%20!=0)return;boolean changed=false;for(var pair:store.data.mounts.entrySet()){
            var entry=pair.getValue();if(!entry.state.equals("activa"))continue;boolean found=false;for(var w:s.getWorlds())if(w.getEntity(java.util.UUID.fromString(entry.entity)) instanceof CrystalMount){found=true;break;}
            if(found){absent.remove(pair.getKey());continue;}int seconds=absent.merge(pair.getKey(),1,Integer::sum);
            // Unloaded or interrupted spawns become a stored reference. Old entities fail their UUID check on load.
            if(seconds>=15){entry.state=entry.temporary?"muerta":"guardada";entry.entity="";absent.remove(pair.getKey());changed=true;Hardcore.LOG.info("Recovered inactive crystal mount {} without healing",pair.getKey());}
        }if(changed)store.save();});
        ServerLifecycleEvents.SERVER_STOPPING.register(s->{if(store!=null)store.save();});
        ServerLifecycleEvents.SERVER_STOPPED.register(s->{store=null;});
        ServerPlayConnectionEvents.DISCONNECT.register((h,s)->{for(var w:s.getWorlds())for(var m:w.getEntitiesByClass(CrystalMount.class,h.player.getBoundingBox().expand(128),m->h.player.getUuid().toString().equals(m.holder()))){boolean rider=m.getFirstPassenger()==h.player;h.player.stopRiding();m.requestLanding();if(rider)m.rememberReturn();}});
        ServerPlayConnectionEvents.JOIN.register((h,sender,s)->{if(store==null)return;for(var m:store.data.mounts.values())if(m.returnPending&&m.holder.equals(h.player.getUuidAsString())){var w=s.getWorld(net.minecraft.registry.RegistryKey.of(net.minecraft.registry.RegistryKeys.WORLD,new net.minecraft.util.Identifier(m.dimension)));if(w==null)continue;var at=new net.minecraft.util.math.Vec3d(m.returnX,m.returnY,m.returnZ);var pos=net.minecraft.util.math.BlockPos.ofFloored(at);if(!w.getBlockState(pos.down()).isSolidBlock(w,pos.down())||!w.getFluidState(pos).isEmpty()||!w.isSpaceEmpty(h.player,h.player.getBoundingBox().offset(at.subtract(h.player.getPos())))||!w.getWorldBorder().contains(pos))continue;h.player.teleport(w,at.x,at.y,at.z,h.player.getYaw(),h.player.getPitch());h.player.fallDistance=0;m.returnPending=false;store.save();break;}});
    }
}
