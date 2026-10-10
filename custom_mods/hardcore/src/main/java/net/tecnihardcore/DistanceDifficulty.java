package net.tecnihardcore;

import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import java.util.*;

/** Applies bounded server-owned hostile health, damage and natural reinforcement by distance. */
public final class DistanceDifficulty {
    private static final UUID HEALTH_ID=UUID.fromString("2c4d60d5-f44e-4f9b-a4dc-9a00ee8d2900");
    private static final ArrayDeque<HostileEntity> pending=new ArrayDeque<>();
    private static int ticks;
    private DistanceDifficulty() {}
    public static void init() {
        ServerEntityEvents.ENTITY_LOAD.register((entity,world)->{
            if(entity instanceof HostileEntity mob && !(mob instanceof TrialBoss))pending.add(mob);
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server->pending.clear());
        ServerTickEvents.END_SERVER_TICK.register(server->{
            for(int i=0;i<16&&!pending.isEmpty();i++){
                HostileEntity mob=pending.poll();if(mob==null||mob.isRemoved()||!(mob.getWorld() instanceof ServerWorld world))continue;
                scaleHealth(mob);reinforce(mob,world);
            }
            if(++ticks%200!=0)return;
            for(var player:server.getPlayerManager().getPlayerList()){
                if(!(player.getWorld() instanceof ServerWorld world))continue;
                for(HostileEntity mob:world.getEntitiesByClass(HostileEntity.class,new Box(player.getBlockPos()).expand(64),m->!(m instanceof TrialBoss)))scaleHealth(mob);
            }
        });
    }
    public static float damage(DamageSource source,float amount) {
        if(!(source.getAttacker() instanceof HostileEntity mob)||mob instanceof TrialBoss||!(mob.getWorld() instanceof ServerWorld))return amount;
        double factor=PhaseRules.multiplier(PhaseRules.ring(mob.getX(),mob.getZ()),PhaseDay.day());
        return (float)Math.min(1000,amount*factor);
    }
    private static void scaleHealth(HostileEntity mob) {
        EntityAttributeInstance attr=mob.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);if(attr==null)return;
        double factor=PhaseRules.multiplier(PhaseRules.ring(mob.getX(),mob.getZ()),PhaseDay.day());
        EntityAttributeModifier current=attr.getModifier(HEALTH_ID);
        if(current==null&&factor==1)return;
        if(current!=null&&Math.abs(current.getValue()-(factor-1))<.0001)return;
        float fraction=mob.getHealth()/Math.max(1,mob.getMaxHealth());
        if(current!=null)attr.removeModifier(HEALTH_ID);
        if(factor>1)attr.addPersistentModifier(new EntityAttributeModifier(HEALTH_ID,"TecniHardcore distance and day",factor-1,EntityAttributeModifier.Operation.MULTIPLY_TOTAL));
        mob.setHealth(Math.max(1,mob.getMaxHealth()*fraction));
    }
    private static void reinforce(HostileEntity mob,ServerWorld world) {
        if(!mob.addCommandTag("tecni:spawn_checked"))return;
        if(mob.age>5||mob.hasCustomName()||SpawnProtection.inside(world,mob.getBlockPos()))return;
        int ring=PhaseRules.ring(mob.getX(),mob.getZ());double chance=PhaseRules.extraSpawnChance(ring);
        if(chance==0||world.random.nextDouble()>=chance)return;
        BlockPos pos=mob.getBlockPos().add(world.random.nextBoolean()?2:-2,0,world.random.nextBoolean()?2:-2);
        if(!world.isChunkLoaded(pos)||!world.getBlockState(pos).isAir()||!world.getBlockState(pos.up()).isAir()||!world.getBlockState(pos.down()).isSolidBlock(world,pos.down()))return;
        if(world.getEntitiesByClass(HostileEntity.class,new Box(pos).expand(12),m->true).size()>=18)return;
        Entity copy=mob.getType().create(world);if(!(copy instanceof HostileEntity extra))return;
        extra.addCommandTag("tecni:spawn_checked");extra.refreshPositionAndAngles(pos,world.random.nextFloat()*360,0);
        extra.initialize(world,world.getLocalDifficulty(pos),SpawnReason.REINFORCEMENT,null,null);
        world.spawnEntity(extra);
    }
}
