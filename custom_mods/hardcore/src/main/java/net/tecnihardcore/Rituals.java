package net.tecnihardcore;
import net.minecraft.registry.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.*;
import net.minecraft.nbt.*;
import net.minecraft.util.Identifier;
import net.minecraft.util.WorldSavePath;
import java.nio.file.*;
import java.util.*;

public final class Rituals {
    private static final class Ritual {
        final UUID id=UUID.randomUUID(),caster,target;final RegistryKey<World> dimension;final BlockPos altar;final Hand hand;final int startTick;
        final String name;boolean committed;net.minecraft.util.math.Vec3d anchor,landing;
        Ritual(ServerPlayerEntity c,ServerPlayerEntity t,BlockPos p,Hand h){caster=c.getUuid();target=t.getUuid();dimension=c.getWorld().getRegistryKey();altar=p;hand=h;startTick=c.getServer().getTicks();name=t.getName().getString();anchor=c.getPos();landing=t.getPos();}
    }
    private static final Map<UUID,Ritual> active=new HashMap<>();
    public static void recoverPayments(MinecraftServer server) {
        try {
            for(var payment:new ArrayList<>(Hardcore.souls.data.ritualPayments.entrySet())) {
                Path file=server.getSavePath(WorldSavePath.PLAYERDATA).resolve(payment.getValue()+".dat");
                if(!Files.exists(file))throw new IllegalStateException("Missing ritual payer data");
                NbtCompound data=NbtIo.readCompressed(file.toFile());NbtList inventory=data.getList("Inventory",NbtElement.COMPOUND_TYPE);
                RitualPayment.recover(inventory,payment.getKey());
                Path tmp=file.resolveSibling(file.getFileName()+".ritual-tmp");NbtIo.writeCompressed(data,tmp.toFile());
                Files.move(tmp,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
                Hardcore.souls.data.ritualPayments.remove(payment.getKey());Hardcore.souls.save();
                Hardcore.LOG.warn("Recovered single ritual payment {}",payment.getKey());
            }
        }catch(Exception e){throw new IllegalStateException("Cannot recover ritual payment; refusing startup",e);}
    }
    public static void cleanOrphanMarkers(ServerPlayerEntity p) {
        for(int i=0;i<p.getInventory().size();i++) {
            var stack=p.getInventory().getStack(i);
            if(stack.hasNbt()&&stack.getNbt().contains("TecniRitualPayment")&&!Hardcore.souls.data.ritualPayments.containsKey(stack.getNbt().getString("TecniRitualPayment")))stack.getNbt().remove("TecniRitualPayment");
        }
    }
    public static void finishRecovery(ServerPlayerEntity player) {
        var soul=Hardcore.soul(player);if(soul.revivePosition==null||!AuthBootstrap.authenticated(player))return;
        var world=player.getServer().getWorld(RegistryKey.of(RegistryKeys.WORLD,new Identifier(soul.reviveDimension)));
        if(world==null)throw new IllegalStateException("Missing resurrection dimension");
        BlockPos safe=new BlockPos(soul.revivePosition[0],soul.revivePosition[1],soul.revivePosition[2]);
        if(!safe(world,safe))safe=world.getSpawnPos();
        player.teleport(world,safe.getX()+.5,safe.getY(),safe.getZ()+.5,player.getYaw(),0);player.changeGameMode(GameMode.SURVIVAL);
        player.setHealth(player.getMaxHealth());player.getHungerManager().setFoodLevel(20);player.getServer().getPlayerManager().saveAllPlayerData();
        soul.revivePosition=null;soul.reviveDimension=null;Hardcore.souls.save();
    }
    public static boolean near(ServerPlayerEntity p,BlockPos altar){return p.squaredDistanceTo(altar.getX()+.5,altar.getY()+.5,altar.getZ()+.5)<=16;}
    private static void message(ServerPlayerEntity p,String text){p.sendMessage(Text.literal(text),false);}
    public static String casterProblem(ServerPlayerEntity p,BlockPos altar) {
        if(Hardcore.soul(p).lives<=0||p.isSpectator()||p.isCreative())return "Necesitas estar en supervivencia y tener vidas.";
        if(!p.getMainHandStack().isOf(Hardcore.HEART)&&!p.getOffHandStack().isOf(Hardcore.HEART))return "Sostén un Corazón Sagrado en una mano.";
        if(!near(p,altar)||p.hurtTime>0)return "Acércate al núcleo y espera a dejar de recibir daño.";
        if(active.values().stream().anyMatch(r->r.dimension==p.getWorld().getRegistryKey()&&r.altar.equals(altar)))return "Este santuario está ocupado.";
        return null;
    }
    public static int begin(ServerPlayerEntity c,ServerPlayerEntity t){
        BlockPos altar=Sanctuaries.nearby(c);if(altar==null){message(c,"Necesitas un Santuario de las Almas. Un bloque de netherita no es un altar.");return 0;}return beginAt(c,t,altar);
    }
    public static int beginAt(ServerPlayerEntity c,ServerPlayerEntity t,BlockPos altar) {
        if(!AuthBootstrap.authenticated(c)||!AuthBootstrap.authenticated(t))return 0;
        if(!RitualNetwork.compatible(c)||!RitualNetwork.compatible(t)){message(c,"Ambos necesitan TecniHardcore 2.2.0. Actualiza el launcher.");return 0;}
        if(!c.getWorld().getBlockState(altar).isOf(Sanctuaries.CORE)){message(c,"El núcleo ya no existe.");return 0;}
        String problem=casterProblem(c,altar);if(problem!=null){message(c,problem);return 0;}
        if(c==t||Hardcore.soul(t).lives!=0||!t.isSpectator()||c.getWorld()!=t.getWorld()||!near(t,altar)){message(c,"Elige un eliminado cercano, en espectador y autenticado.");return 0;}
        if(active.values().stream().anyMatch(r->r.caster.equals(c.getUuid())||r.target.equals(t.getUuid())||r.target.equals(c.getUuid())||r.caster.equals(t.getUuid()))){message(c,"Ya hay un ritual en curso para estos jugadores.");return 0;}
        Hand hand=c.getMainHandStack().isOf(Hardcore.HEART)?Hand.MAIN_HAND:Hand.OFF_HAND;
        Ritual r=new Ritual(c,t,altar,hand);active.put(t.getUuid(),r);broadcast(c.getServer(),r,0,0);
        message(c,"Ritual iniciado: sostén el corazón y permanece junto al núcleo durante 30 segundos.");message(t,"Tu alma está regresando. Permanece junto al santuario.");return 1;
    }
    private static void broadcast(MinecraftServer s,Ritual r,int elapsed,int state){
        var c=s.getPlayerManager().getPlayer(r.caster);if(c!=null&&!r.committed)r.anchor=c.getPos();
        var w=s.getWorld(r.dimension);if(w!=null)for(var v:s.getPlayerManager().getPlayerList()){
            boolean participant=v.getUuid().equals(r.caster)||v.getUuid().equals(r.target);
            if(participant||(v.getWorld()==w&&v.squaredDistanceTo(r.altar.getX()+.5,r.altar.getY()+.5,r.altar.getZ()+.5)<4096))RitualNetwork.visual(v,r.id,r.altar,r.caster,r.target,r.anchor,r.landing,elapsed,state,r.name);
        }
    }
    public static void cancelFor(UUID id){cancel(r->r.caster.equals(id)||r.target.equals(id));RitualNetwork.forget(id);}
    public static void cancelAt(RegistryKey<World> dim,BlockPos pos){cancel(r->r.dimension==dim&&r.altar.equals(pos));}
    private static void cancel(java.util.function.Predicate<Ritual> match){
        var it=active.values().iterator();while(it.hasNext()){var r=it.next();if(match.test(r)&&!r.committed){it.remove();cancelled(Hardcore.server,r);}}
    }
    private static void cancelled(MinecraftServer s,Ritual r){
        broadcast(s,r,Math.max(0,s.getTicks()-r.startTick),2);
        for(UUID id:List.of(r.caster,r.target)){var p=s.getPlayerManager().getPlayer(id);if(p!=null)message(p,"Ritual cancelado. No se ha consumido el corazón.");}
    }
    private static boolean safe(ServerWorld w,BlockPos p){return w.getBlockState(p.down()).isSolidBlock(w,p.down())&&w.getBlockState(p).isAir()&&w.getBlockState(p.up()).isAir()&&w.getWorldBorder().contains(p)
        &&w.getOtherEntities(null,new net.minecraft.util.math.Box(p.getX()+.2,p.getY(),p.getZ()+.2,p.getX()+.8,p.getY()+1.8,p.getZ()+.8),e->e instanceof net.minecraft.entity.LivingEntity&&!e.isSpectator()).isEmpty();}
    public static void tick(MinecraftServer s){
        var it=active.values().iterator();while(it.hasNext()){
            Ritual r=it.next();int elapsed=s.getTicks()-r.startTick;
            if(r.committed){if(elapsed>=680){broadcast(s,r,elapsed,3);it.remove();}else if(elapsed%10==0)broadcast(s,r,elapsed,1);continue;}
            var c=s.getPlayerManager().getPlayer(r.caster);var t=s.getPlayerManager().getPlayer(r.target);var w=s.getWorld(r.dimension);
            boolean valid=c!=null&&t!=null&&w!=null&&c.getWorld()==w&&t.getWorld()==w&&AuthBootstrap.authenticated(c)&&AuthBootstrap.authenticated(t)
                &&near(c,r.altar)&&near(t,r.altar)&&w.getBlockState(r.altar).isOf(Sanctuaries.CORE)&&c.hurtTime==0&&t.hurtTime==0
                &&!c.isSpectator()&&!c.isCreative()&&Hardcore.soul(c).lives>0&&t.isSpectator()&&Hardcore.soul(t).lives==0&&c.getStackInHand(r.hand).isOf(Hardcore.HEART);
            if(!valid){it.remove();cancelled(s,r);continue;}
            if(elapsed%10==0)broadcast(s,r,elapsed,0);
            if(elapsed<600)continue;
            net.minecraft.util.math.Vec3d front=c.getPos().add(net.minecraft.util.math.Vec3d.fromPolar(0,c.getYaw()).multiply(1.4));
            BlockPos safe=null;double nearest=Double.MAX_VALUE;
            for(BlockPos p:BlockPos.iterate(r.altar.add(-3,0,-3),r.altar.add(3,1,3)))if(safe(w,p)){
                double score=p.toCenterPos().squaredDistanceTo(front);if(score<nearest){nearest=score;safe=p.toImmutable();}
            }
            if(safe==null){it.remove();cancelled(s,r);message(c,"No hay espacio seguro. Libera suelo y dos bloques de altura junto al núcleo.");continue;}
            String payment=r.id.toString();var offering=c.getStackInHand(r.hand);offering.getOrCreateNbt().putString("TecniRitualPayment",payment);s.getPlayerManager().saveAllPlayerData();
            var soul=Hardcore.soul(t);Hardcore.souls.data.ritualPayments.put(payment,c.getUuidAsString());soul.resurrections++;soul.revived=true;soul.lives=1;
            soul.reviveDimension=w.getRegistryKey().getValue().toString();soul.revivePosition=new int[]{safe.getX(),safe.getY(),safe.getZ()};Hardcore.souls.save();
            offering.decrement(1);if(offering.hasNbt())offering.getNbt().remove("TecniRitualPayment");
            t.teleport(w,safe.getX()+.5,safe.getY(),safe.getZ()+.5,t.getYaw(),0);t.changeGameMode(GameMode.SURVIVAL);t.setHealth(t.getMaxHealth());t.getHungerManager().setFoodLevel(20);
            r.anchor=c.getPos();r.landing=t.getPos();
            t.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(net.minecraft.entity.effect.StatusEffects.RESISTANCE,80,4,false,false));
            s.getPlayerManager().saveAllPlayerData();soul.revivePosition=null;soul.reviveDimension=null;Hardcore.souls.data.ritualPayments.remove(payment);Hardcore.souls.save();
            r.committed=true;Hardcore.send(t);Hardcore.publish();broadcast(s,r,600,1);RitualNetwork.relic(t,3);
            Hardcore.LOG.info("Ritual {} committed: {} resurrected {} (total {})",r.id,c.getUuid(),t.getUuid(),soul.resurrections);
            message(c,"Un corazón consumido. Resurrección completada.");message(t,"Has vuelto con una vida. Resurrecciones realizadas: "+soul.resurrections+". Puedes volver mediante otro ritual si quedas eliminado.");
        }
    }
}
