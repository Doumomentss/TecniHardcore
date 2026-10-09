package net.tecnihardcore;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import java.nio.file.*;
/** Explicitly opt-in test host command spool, never enabled by the production start script. */
public final class ExpansionQa {
    private static long tickStart;private static int remaining;private static String benchmark="";private static final java.util.List<Double> tickTimes=new java.util.ArrayList<>();
    private static net.minecraft.server.world.ServerWorld countWorld;
    private static long countIndex,countSolid,countFluid,countTotal;
    private static int countX,countY,countZ,countSX,countSY,countSZ;
    private static String countName;
    public static void init(){if(!Boolean.getBoolean("tecni.testServer"))return;
        ServerTickEvents.START_SERVER_TICK.register(s->tickStart=System.nanoTime());
        ServerTickEvents.END_SERVER_TICK.register(s->{if(countWorld==null)return;long deadline=System.nanoTime()+2_000_000;var pos=new net.minecraft.util.math.BlockPos.Mutable();
            for(int i=0;i<20000&&countIndex<countTotal&&System.nanoTime()<deadline;i++,countIndex++){
                pos.set(countX+(int)(countIndex%countSX),countY+(int)(countIndex/countSX%countSY),countZ+(int)(countIndex/(countSX*(long)countSY)));
                if(!countWorld.isChunkLoaded(pos)){countWorld=null;throw new IllegalStateException("QA count requires already loaded chunks");}
                var state=countWorld.getBlockState(pos);if(state.getBlock() instanceof net.minecraft.block.FluidBlock)countFluid++;else if(!state.isAir()&&!state.isOf(net.minecraft.block.Blocks.STRUCTURE_VOID))countSolid++;
            }
            if(countIndex==countTotal)try{var json=new com.google.gson.JsonObject();json.addProperty("solid",countSolid);json.addProperty("fluid",countFluid);json.addProperty("volume",countTotal);Path directory=s.getRunDirectory().toPath().resolve("qa-results");Files.createDirectories(directory);Files.writeString(directory.resolve(countName+".json"),json.toString());countWorld=null;}catch(Exception error){countWorld=null;Hardcore.LOG.error("QA count failed",error);}
        });
        ServerTickEvents.END_SERVER_TICK.register(s->{if(remaining<=0)return;tickTimes.add((System.nanoTime()-tickStart)/1e6);if(--remaining==0)try{var sorted=new java.util.ArrayList<>(tickTimes);java.util.Collections.sort(sorted);var json=new com.google.gson.JsonObject();json.addProperty("ticks",sorted.size());json.addProperty("meanMs",sorted.stream().mapToDouble(Double::doubleValue).average().orElse(0));json.addProperty("p95Ms",sorted.get(Math.min(sorted.size()-1,(int)Math.ceil(sorted.size()*.95)-1)));json.addProperty("maxMs",sorted.get(sorted.size()-1));Path directory=s.getRunDirectory().toPath().resolve("qa-results");Files.createDirectories(directory);Files.writeString(directory.resolve(benchmark+"-ticks.json"),json.toString());}catch(Exception error){Hardcore.LOG.error("QA benchmark failed",error);}});
        ServerTickEvents.END_SERVER_TICK.register(s->{if(s.getTicks()%10!=0)return;Path p=s.getRunDirectory().toPath().resolve("qa-commands.txt");try{if(!Files.exists(p))return;var commands=Files.readAllLines(p);Files.delete(p);for(String line:commands)if(!line.isBlank()){Hardcore.LOG.info("QA command: {}",line);if(line.startsWith("qa25 "))probe(s,line.substring(5));else s.getCommandManager().executeWithPrefix(s.getCommandSource(),line);}}catch(Exception e){Hardcore.LOG.error("QA spool failed",e);}});
    }
    private static void probe(net.minecraft.server.MinecraftServer s,String command)throws Exception{
        if(!Files.exists(s.getRunDirectory().toPath().resolve(".tecni-test-world")))throw new IllegalStateException("Isolated-world marker required");
        String[] args=command.split(" ");if(args.length<2||!args[1].matches("E25[A-Za-z0-9_]{1,13}"))throw new IllegalArgumentException("Only E25 test accounts may be changed");
        var p=s.getPlayerManager().getPlayer(args[1]);if(p==null)throw new IllegalArgumentException("Test player not connected");
        if(args[0].equals("authenticate")&&AuthBootstrap.enabled()){
            var auth=(xyz.nikitacartes.easyauth.utils.PlayerAuth)p;
            auth.easyAuth$setSkipAuth();auth.easyAuth$setAuthenticated(true);auth.easyAuth$setKickTimer(Long.MAX_VALUE);
        }
        if(args[0].equals("protection-clear")){Moderation.store.data.protection.remove(p.getUuidAsString());Moderation.store.save();}
        if(args[0].equals("social-reset"))Social.store.commit(new SocialStore.Business(),"isolated QA reset");
        if(args[0].equals("civic")){var npc=CivicNpcs.find(args[2]);if(npc==null)throw new IllegalArgumentException("NPC not loaded");CivicNetwork.dialogue(p,npc,"inicio");}
        if(args[0].equals("social-report")){var json=new com.google.gson.JsonObject();json.addProperty("balance",Market.balance(p.getUuidAsString()));json.addProperty("team",Social.teamId(p.getUuidAsString()));json.addProperty("inventory",p.getInventory().writeNbt(new net.minecraft.nbt.NbtList()).toString());json.addProperty("health",p.getHealth());json.addProperty("npcCount",CivicNpcs.definitions.size());Path directory=s.getRunDirectory().toPath().resolve("qa-results");Files.createDirectories(directory);Files.writeString(directory.resolve(args[1]+"-"+args[2]+".json"),json.toString());}
        if(args[0].equals("volume")){
            if(countWorld!=null||!args[2].matches("[a-z0-9-]{1,40}"))throw new IllegalArgumentException("QA volume busy or invalid name");
            countX=Integer.parseInt(args[3]);countY=Integer.parseInt(args[4]);countZ=Integer.parseInt(args[5]);
            countSX=Integer.parseInt(args[6]);countSY=Integer.parseInt(args[7]);countSZ=Integer.parseInt(args[8]);
            countTotal=(long)countSX*countSY*countSZ;
            if(countSX<1||countSX>160||countSY<1||countSY>320||countSZ<1||countSZ>160||countY<p.getWorld().getBottomY()||countY+countSY>p.getWorld().getTopY())throw new IllegalArgumentException("QA volume bounds");
            countIndex=countSolid=countFluid=0;countName=args[1]+"-"+args[2];countWorld=p.getServerWorld();
        }
        if(args[0].equals("terrain")){
            int x=Integer.parseInt(args[3]),z=Integer.parseInt(args[4]),air=0,surface=0;
            for(int dx=-12;dx<=12;dx++)for(int dz=-12;dz<=12;dz++)for(int y=80;y<=95;y++){
                var pos=new net.minecraft.util.math.BlockPos(x+dx,y,z+dz);
                if(!p.getServerWorld().isChunkLoaded(pos))throw new IllegalStateException("QA terrain must already be loaded");
                if(p.getServerWorld().getBlockState(pos).isAir()){air++;if(y==95)surface++;}
            }
            var result=new com.google.gson.JsonObject();result.addProperty("air",air);result.addProperty("surface",surface);
            Path directory=s.getRunDirectory().toPath().resolve("qa-results");Files.createDirectories(directory);Files.writeString(directory.resolve(args[1]+"-"+args[2]+".json"),result.toString());
        }
        if(args[0].equals("benchmark")){if(!args[2].matches("[a-z0-9-]{1,40}"))throw new IllegalArgumentException("Benchmark name");benchmark=args[2];remaining=Math.max(20,Math.min(2400,Integer.parseInt(args[3])));tickTimes.clear();}
        if(args[0].equals("lethal"))p.damage(p.getDamageSources().generic(),100000);
        if(args[0].equals("boss-hit")){for(var e:p.getServerWorld().iterateEntities())if(e instanceof TrialBoss b&&b.isAlive()&&b.squaredDistanceTo(p)<96*96){b.damage(p.getDamageSources().playerAttack(p),Float.parseFloat(args[2]));break;}}
        if(args[0].equals("reward-partial")){String id="qa-partial:"+args[2]+":"+p.getUuidAsString();var r=new ExpansionStore.Reward();r.player=p.getUuidAsString();r.reason="Isolated interrupted delivery";r.diamonds=4;r.gold=1;r.iron=2;r.state="delivering";Expansion.store.data.rewards.put(id,r);Expansion.store.save();var item=new net.minecraft.item.ItemStack(net.minecraft.item.Items.DIAMOND,4);item.getOrCreateNbt().putString("TecniReward",id);item.getOrCreateNbt().putInt("TecniRewardPart",0);p.getInventory().setStack(0,item);s.getPlayerManager().saveAllPlayerData();}
        if(args[0].equals("mount")){int tier=Integer.parseInt(args[2]);for(int slot=0;slot<36;slot++){var item=p.getInventory().getStack(slot);if(item.isOf(CrystalMount.TOKEN)&&item.hasNbt()&&item.getNbt().getInt("CrystalTier")==tier){p.getInventory().selectedSlot=slot<9?slot:0;if(slot>=9){var swap=p.getInventory().getStack(0);p.getInventory().setStack(0,item);p.getInventory().setStack(slot,swap);}CrystalMount.TOKEN.use(p.getWorld(),p,net.minecraft.util.Hand.MAIN_HAND);for(var e:p.getServerWorld().iterateEntities())if(e instanceof CrystalMount m&&m.holder().equals(p.getUuidAsString())){p.startRiding(m);break;}break;}}}
        if(args[0].equals("mount-damage")&&p.getVehicle() instanceof CrystalMount m)m.damage(p.getDamageSources().generic(),Float.parseFloat(args[2]));
        if(args[0].equals("mount-interact")){for(var entity:p.getServerWorld().iterateEntities())if(entity instanceof CrystalMount m&&m.holder().equals(p.getUuidAsString())){p.stopRiding();if(args[2].equals("feed")){p.getInventory().selectedSlot=8;p.getInventory().setStack(8,new net.minecraft.item.ItemStack(net.minecraft.item.Items.AMETHYST_SHARD,8));}p.setSneaking(args[2].equals("store"));m.interactMob(p,net.minecraft.util.Hand.MAIN_HAND);p.setSneaking(false);break;}}
        if(args[0].equals("mount-land")){if(p.getVehicle() instanceof CrystalMount m){p.stopRiding();m.requestLanding();}}
        if(args[0].equals("mount-clone")){var source=p.getInventory().getStack(p.getInventory().selectedSlot);if(source.isOf(CrystalMount.TOKEN))p.getInventory().setStack(7,source.copy());}
        if(args[0].equals("reward")){String id="qa:"+args[2]+":"+p.getUuidAsString();if(!Expansion.store.data.rewards.containsKey(id)){var r=new ExpansionStore.Reward();r.player=p.getUuidAsString();r.reason="Isolated journal test";r.diamonds=4;r.gold=1;r.iron=2;r.trophy="Trofeo de prueba";Expansion.store.data.rewards.put(id,r);Expansion.store.save();}}
        if(args[0].equals("report")){var report=new com.google.gson.JsonObject();report.addProperty("name",p.getName().getString());report.addProperty("lives",Hardcore.soul(p).lives);report.addProperty("health",p.getHealth());report.addProperty("mode",p.interactionManager.getGameMode().getName());report.addProperty("dimension",p.getWorld().getRegistryKey().getValue().toString());report.addProperty("inventory",p.getInventory().writeNbt(new net.minecraft.nbt.NbtList()).toString());report.addProperty("x",p.getX());report.addProperty("y",p.getY());report.addProperty("z",p.getZ());if(p.getVehicle() instanceof CrystalMount m){report.addProperty("mount",m.registryId());report.addProperty("mountEntity",m.getUuidAsString());report.addProperty("mountTier",m.tier());report.addProperty("mountHealth",m.getHealth());report.addProperty("mountX",m.getX());report.addProperty("mountY",m.getY());report.addProperty("mountZ",m.getZ());}int mounts=0;for(var e:p.getServerWorld().iterateEntities())if(e instanceof CrystalMount m&&m.holder().equals(p.getUuidAsString())&&m.isAlive())mounts++;report.addProperty("activeEntities",mounts);Path directory=s.getRunDirectory().toPath().resolve("qa-results");Files.createDirectories(directory);Files.writeString(directory.resolve(args[1]+"-"+args[2]+".json"),report.toString(),java.nio.charset.StandardCharsets.UTF_8);}
    }
}
