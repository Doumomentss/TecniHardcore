package net.tecnihardcore;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.*;
import net.minecraft.entity.projectile.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.*;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.*;
import net.minecraft.world.World;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;
public final class TrialShard extends ProjectileEntity implements GeoEntity {
    public static final EntityType<TrialShard> TYPE=Registry.register(Registries.ENTITY_TYPE,Hardcore.id("trial_shard"),FabricEntityTypeBuilder.create(SpawnGroup.MISC,TrialShard::new).dimensions(EntityDimensions.fixed(.45F,.45F)).trackRangeBlocks(80).trackedUpdateRate(1).build());
    private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
    private java.util.UUID target;
    public void track(java.util.UUID target){this.target=target;}
    public TrialShard(EntityType<? extends TrialShard> type,World w){super(type,w);}
    public static void init(){}
    @Override protected void initDataTracker(){}
    @Override public void tick(){super.tick();if(!getWorld().isClient&&(!TrialBoss.enabled()||!(getOwner() instanceof TrialBoss boss)||!boss.isAlive())){discard();return;}
        if(target!=null&&getWorld() instanceof ServerWorld w&&age<40){var p=w.getServer().getPlayerManager().getPlayer(target);if(p!=null&&p.getWorld()==w&&!p.isSpectator()&&AuthBootstrap.authenticated(p)){var desired=p.getPos().add(0,1,0).subtract(getPos()).normalize().multiply(.7);setVelocity(getVelocity().multiply(.92).add(desired.multiply(.08)));velocityDirty=true;}}
        HitResult hit=ProjectileUtil.getCollision(this,e->e instanceof net.minecraft.server.network.ServerPlayerEntity p&&!p.isCreative()&&!p.isSpectator()&&AuthBootstrap.authenticated(p)&&!SpawnProtection.inside(p.getWorld(),p.getBlockPos())&&e!=getOwner());if(hit.getType()!=HitResult.Type.MISS){if(!getWorld().isClient){if(hit instanceof EntityHitResult h&&getOwner() instanceof TrialBoss boss)h.getEntity().damage(getDamageSources().mobProjectile(this,boss),BossCombat.damage(target==null?3:9));discard();}return;}setPosition(getPos().add(getVelocity()));if(getWorld() instanceof ServerWorld w&&age%2==0)w.spawnParticles(SanctuaryEffects.AZURE,getX(),getY(),getZ(),2,.05,.05,.05,.015);if(age>=(target==null?100:80))discard();}
    @Override protected void writeCustomDataToNbt(NbtCompound n){super.writeCustomDataToNbt(n);}
    @Override protected void readCustomDataFromNbt(NbtCompound n){super.readCustomDataFromNbt(n);discard();}
    @Override public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar r){r.add(new AnimationController<>(this,"shard",0,s->s.setAndContinue(RawAnimation.begin().thenLoop("animation.shard.spin"))));}
}
