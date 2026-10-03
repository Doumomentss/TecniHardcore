package net.tecnihardcore;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.item.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.random.Random;
import java.util.*;
import static net.minecraft.server.command.CommandManager.literal;

/** Journal plus inventory delivery markers: recovery happens before the user can act. */
public final class Rewards {
    public static void init(){
        CommandRegistrationCallback.EVENT.register((d,a,e)->d.register(literal("tecni").then(literal("recompensas").executes(c->claim(c.getSource().getPlayerOrThrow())))));
    }
    public static void boss(TrialBoss boss,Map<String,Double> contributions,double total){
        boolean test=Boolean.getBoolean("tecni.testServer");
        boolean isolatedLoot=test&&Boolean.getBoolean("tecni.testRewards")&&java.nio.file.Files.exists(((net.minecraft.server.world.ServerWorld)boss.getWorld()).getServer().getRunDirectory().toPath().resolve(".tecni-test-world"));
        if(Expansion.store==null||test&&!isolatedLoot||boss.getWorld().getRegistryKey()==EventDirector.DIMENSION)return;
        var data=Expansion.store.data;String encounter=boss.getUuidAsString();if(!data.defeated.add(encounter))return;
        var random=boss.getRandom();
        for(var p:((net.minecraft.server.world.ServerWorld)boss.getWorld()).getPlayers(p->AuthBootstrap.authenticated(p)&&p.squaredDistanceTo(boss)<=96*96)){
            double contribution=contributions.getOrDefault(p.getUuidAsString(),0.0);
            if(!ExpansionRules.eligible(contribution,total)){if(contribution>0)p.sendMessage(Text.literal("Custodio: no alcanzaste el 5 % de daño efectivo para recibir botín."),false);continue;}
            var reward=new ExpansionStore.Reward();reward.player=p.getUuidAsString();reward.diamonds=4+random.nextInt(5);reward.gold=1+random.nextInt(3);reward.iron=2+random.nextInt(3);reward.reason="Custodio · "+encounter;
            int tier=ExpansionRules.mountRoll(random.nextInt(100));if(tier>0){String id=UUID.randomUUID().toString();var m=new ExpansionStore.Mount();m.tier=tier;m.health=ExpansionRules.HEALTH[tier-1];data.mounts.put(id,m);reward.mount=id;}
            data.rewards.put(encounter+":"+p.getUuidAsString(),reward);
            p.sendMessage(Text.literal("Botín personal: "+reward.diamonds+" diamantes, "+reward.gold+" bloques de oro, "+reward.iron+" de hierro"+(tier==0?". Sin montura en esta tirada.":" y "+ExpansionRules.MOUNTS[tier-1]+".")+" Reclama con /tecni recompensas."),false);
        }
        Expansion.store.save();
    }
    public static void event(String run,ServerPlayerEntity p,int diamonds,String trophy){String id="event:"+run+":"+p.getUuidAsString();if(Expansion.store.data.rewards.containsKey(id))return;var r=new ExpansionStore.Reward();r.player=p.getUuidAsString();r.reason="Evento "+run;r.diamonds=diamonds;r.trophy=trophy;Expansion.store.data.rewards.put(id,r);Expansion.store.save();}
    private static List<ItemStack> items(String id,ExpansionStore.Reward r){List<ItemStack> list=new ArrayList<>();if(r.diamonds>0)list.add(new ItemStack(Items.DIAMOND,r.diamonds));if(r.gold>0)list.add(new ItemStack(Items.GOLD_BLOCK,r.gold));if(r.iron>0)list.add(new ItemStack(Items.IRON_BLOCK,r.iron));if(!r.mount.isEmpty())list.add(CrystalMount.token(r.mount));if(!r.trophy.isEmpty()){var trophy=new ItemStack(EventDirector.TROPHY);trophy.setCustomName(Text.literal(r.trophy));list.add(trophy);}for(int i=0;i<list.size();i++){var n=list.get(i).getOrCreateNbt();n.putString("TecniReward",id);n.putInt("TecniRewardPart",i);}return list;}
    private static boolean part(ServerPlayerEntity p,String id,int part){for(int slot=0;slot<p.getInventory().size();slot++){var n=p.getInventory().getStack(slot).getNbt();if(n!=null&&id.equals(n.getString("TecniReward"))&&part==n.getInt("TecniRewardPart"))return true;}return false;}
    private static boolean deliver(ServerPlayerEntity p,String id,ExpansionStore.Reward r){var list=items(id,r);int missing=0;for(int i=0;i<list.size();i++)if(!part(p,id,i))missing++;int slots=0;for(int i=0;i<36;i++)if(p.getInventory().getStack(i).isEmpty())slots++;if(slots<missing)return false;
        r.state="delivering";Expansion.store.save();for(int i=0;i<list.size();i++)if(!part(p,id,i))for(int slot=0;slot<36;slot++)if(p.getInventory().getStack(slot).isEmpty()){p.getInventory().setStack(slot,list.get(i));break;}
        p.getServer().getPlayerManager().saveAllPlayerData();r.state="claimed";Expansion.store.save();p.currentScreenHandler.sendContentUpdates();return true;
    }
    public static void recover(ServerPlayerEntity p){for(var entry:Expansion.store.data.rewards.entrySet())if(entry.getValue().player.equals(p.getUuidAsString())&&entry.getValue().state.equals("delivering"))deliver(p,entry.getKey(),entry.getValue());}
    private static int claim(ServerPlayerEntity p){if(!AuthBootstrap.authenticated(p)||EventDirector.participant(p))return 0;int count=0;recover(p);for(var entry:Expansion.store.data.rewards.entrySet()){var r=entry.getValue();if(r.player.equals(p.getUuidAsString())&&r.state.equals("pending")){if(!deliver(p,entry.getKey(),r)){p.sendMessage(Text.literal("Libera al menos cinco espacios del inventario y vuelve a reclamar."),false);break;}count++;}}
        p.sendMessage(Text.literal(count>0?"Recompensas entregadas: "+count:"No hay recompensas nuevas entregadas."),false);return count;
    }
}
