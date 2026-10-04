package net.tecnihardcore;
import net.fabricmc.fabric.api.object.builder.v1.entity.*;
import net.minecraft.entity.*;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.data.*;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.*;
import net.minecraft.util.*;
import net.minecraft.world.World;

public final class CivicNpcEntity extends PathAwareEntity {
    public static final EntityType<CivicNpcEntity> TYPE=Registry.register(Registries.ENTITY_TYPE,Hardcore.id("npc"),FabricEntityTypeBuilder.create(SpawnGroup.MISC,CivicNpcEntity::new).dimensions(EntityDimensions.fixed(.6F,1.8F)).trackRangeBlocks(64).trackedUpdateRate(3).build());
    private static final TrackedData<String> KEY=DataTracker.registerData(CivicNpcEntity.class,TrackedDataHandlerRegistry.STRING),SKIN=DataTracker.registerData(CivicNpcEntity.class,TrackedDataHandlerRegistry.STRING),FALLBACK=DataTracker.registerData(CivicNpcEntity.class,TrackedDataHandlerRegistry.STRING);
    private static final TrackedData<Boolean> SLIM=DataTracker.registerData(CivicNpcEntity.class,TrackedDataHandlerRegistry.BOOLEAN);
    public CivicNpcEntity(EntityType<? extends CivicNpcEntity> type,World world){super(type,world);setAiDisabled(true);setNoGravity(true);setPersistent();setInvulnerable(true);}
    static void init(){FabricDefaultAttributeRegistry.register(TYPE,createMobAttributes().add(net.minecraft.entity.attribute.EntityAttributes.GENERIC_MAX_HEALTH,20));}
    @Override protected void initDataTracker(){super.initDataTracker();dataTracker.startTracking(KEY,"");dataTracker.startTracking(SKIN,"");dataTracker.startTracking(FALLBACK,"steve");dataTracker.startTracking(SLIM,false);}
    public String key(){return dataTracker.get(KEY);}public String skin(){return dataTracker.get(SKIN);}public String fallback(){return dataTracker.get(FALLBACK);}public boolean slim(){return dataTracker.get(SLIM);}
    void configure(String id,CivicNpcs.Definition d){dataTracker.set(KEY,id);dataTracker.set(SKIN,d.skinHash);dataTracker.set(FALLBACK,d.fallback);dataTracker.set(SLIM,d.slim);setCustomName(net.minecraft.text.Text.literal(d.name));setCustomNameVisible(true);}
    @Override public boolean isPushable(){return false;}
    @Override public boolean damage(DamageSource source,float amount){return false;}
    @Override public boolean canImmediatelyDespawn(double distance){return false;}
    @Override public void tick(){super.tick();if(!getWorld().isClient){setVelocity(0,0,0);if(age%20==0){var player=getWorld().getClosestPlayer(this,6);if(player!=null){lookAt(EntityAnchorArgumentType.EntityAnchor.EYES,player.getEyePos());setHeadYaw(getYaw());}}}}
    @Override public ActionResult interactMob(PlayerEntity player,Hand hand){if(player instanceof net.minecraft.server.network.ServerPlayerEntity p&&hand==Hand.MAIN_HAND&&Social.ready(p))CivicNetwork.dialogue(p,this,"inicio");return ActionResult.SUCCESS;}
    @Override public void writeCustomDataToNbt(NbtCompound nbt){super.writeCustomDataToNbt(nbt);nbt.putString("TecniNpc",key());}
    @Override public void readCustomDataFromNbt(NbtCompound nbt){super.readCustomDataFromNbt(nbt);dataTracker.set(KEY,nbt.getString("TecniNpc"));}
}
