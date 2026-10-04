package net.tecnihardcore;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.item.*;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.nbt.*;
import java.util.*;

final class CivicQuests {
    static final Map<String,String> NAMES=Map.of("sendero","Más allá de la plaza","madera","Preparar tu refugio","hierro","Primeras herramientas");
    static final Map<String,Integer> PAY=Map.of("sendero",15,"madera",25,"hierro",40);
    static Set<String> flags(SocialStore.Business b,String player){return b.quests.computeIfAbsent(player,k->new HashSet<>());}
    static boolean eligible(ServerPlayerEntity p){if(!Social.ready(p))return false;if(EventDirector.participant(p)||p.isCreative()||p.isSpectator()||!p.isAlive()||Hardcore.soul(p).lives<=0){Social.say(p,"Las misiones iniciales requieren estar vivo en supervivencia, fuera de un evento.");return false;}return true;}
    static void start(ServerPlayerEntity p,String id){if(!eligible(p)||!NAMES.containsKey(id))return;var b=Social.store.copy();var flags=flags(b,p.getUuidAsString());if(flags.contains(id+":done")){Social.say(p,"Ya completaste esta misión.");return;}if(!flags.add(id+":active")){Social.say(p,"Ya tenés esta misión activa.");return;}Social.store.commit(b,"quest accepted "+p.getUuidAsString()+" "+id);Social.say(p,"Misión: "+NAMES.get(id)+". "+(id.equals("sendero")?"Aléjate 128 bloques del spawn en supervivencia y vuelve.":id.equals("madera")?"Trae 16 troncos para preparar suministros.":"Trae 4 unidades de hierro en bruto."));}
    static void tick(ServerPlayerEntity p){if(Social.store==null||EventDirector.participant(p)||!p.isAlive()||!AuthBootstrap.authenticated(p)||!p.interactionManager.getGameMode().isSurvivalLike()||p.isSpectator()||p.getWorld().getRegistryKey()!=net.minecraft.world.World.OVERWORLD)return;var f=Social.store.data.quests.get(p.getUuidAsString());boolean plaza=SpawnProtection.active(p.getWorld());var spawn=p.getServerWorld().getSpawnPos();double dx=p.getX()-(plaza?0:spawn.getX()),dz=p.getZ()-(plaza?0:spawn.getZ());if(f!=null&&f.contains("sendero:active")&&!f.contains("sendero:ready")&&dx*dx+dz*dz>=128*128){var b=Social.store.copy();flags(b,p.getUuidAsString()).add("sendero:ready");Social.store.commit(b,"quest exploration "+p.getUuidAsString());Social.say(p,"Exploración completada. Vuelve con Inés para recibir 15 Cristales Tecni.");}}
    static void claim(ServerPlayerEntity p,String id){if(!eligible(p)||!NAMES.containsKey(id))return;var b=Social.store.copy();var f=flags(b,p.getUuidAsString());if(!f.contains(id+":active")||f.contains(id+":done")){Social.say(p,"Primero acepta la misión; cada misión inicial se cobra una sola vez.");return;}if(id.equals("sendero")&&!f.contains("sendero:ready")){Social.say(p,"Todavía debes explorar más allá de 128 bloques del spawn.");return;}
        NbtList after=p.getInventory().writeNbt(new NbtList());if(!id.equals("sendero")){int needed=id.equals("madera")?16:4;for(int i=0;i<after.size()&&needed>0;i++){var tag=after.getCompound(i);ItemStack stack=ItemStack.fromNbt(tag);int slot=tag.getByte("Slot")&255;if(slot>=36)continue;boolean accepted=id.equals("madera")?stack.isIn(ItemTags.LOGS):stack.isOf(Items.RAW_IRON);if(accepted){int take=Math.min(needed,stack.getCount());stack.decrement(take);needed-=take;var updated=stack.writeNbt(new NbtCompound());updated.putByte("Slot",(byte)slot);after.set(i,updated);}}if(needed>0){Social.say(p,"Te faltan materiales. La entrega consume 16 troncos o 4 hierros en bruto.");return;}}
        f.remove(id+":active");f.add(id+":done");b.wallets.put(p.getUuidAsString(),SocialRules.transfer(b.wallets.getOrDefault(p.getUuidAsString(),0L),PAY.get(id)));
        if(id.equals("sendero"))Social.store.commit(b,"quest reward "+p.getUuidAsString()+" "+id);else Social.store.inventory(p,after,b,"quest delivery "+id);Social.say(p,"Misión completada: +"+PAY.get(id)+" Cristales Tecni.");
    }
}
