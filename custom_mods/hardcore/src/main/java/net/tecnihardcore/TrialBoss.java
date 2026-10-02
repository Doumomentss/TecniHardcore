package net.tecnihardcore;
import net.fabricmc.fabric.api.object.builder.v1.entity.*;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.entity.*;
import net.minecraft.entity.attribute.*;
import net.minecraft.entity.boss.*;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.data.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.*;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;
import java.util.*;
import static net.minecraft.server.command.CommandManager.*;

/** Deliberately has no spawn egg, natural spawn entry, recipe or production command. */
public final class TrialBoss extends HostileEntity implements GeoEntity {
    public static final EntityType<TrialBoss> TYPE=Registry.register(Registries.ENTITY_TYPE,Hardcore.id("custodio_pizarra"),FabricEntityTypeBuilder.create(SpawnGroup.MONSTER,TrialBoss::new).dimensions(EntityDimensions.fixed(2.2F,4.3F)).trackRangeBlocks(96).trackedUpdateRate(1).build());
    private static final TrackedData<Integer> ATTACK=DataTracker.registerData(TrialBoss.class,TrackedDataHandlerRegistry.INTEGER),AGE=DataTracker.registerData(TrialBoss.class,TrackedDataHandlerRegistry.INTEGER),PHASE=DataTracker.registerData(TrialBoss.class,TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Float> AIM=DataTracker.registerData(TrialBoss.class,TrackedDataHandlerRegistry.FLOAT);
    private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
    private final ServerBossBar bar=new ServerBossBar(Text.literal("Custodio de Pizarra · PROTOTIPO"),BossBar.Color.BLUE,BossBar.Style.NOTCHED_10);
    private final Set<UUID> struck=new HashSet<>();
    private BlockPos home;
    private int pause=60,next=1;
    private Vec3d origin=Vec3d.ZERO;
    public TrialBoss(EntityType<? extends HostileEntity> type,World world){super(type,world);setPersistent();experiencePoints=0;}
    public static void init(){
        FabricDefaultAttributeRegistry.register(TYPE,HostileEntity.createHostileAttributes().add(EntityAttributes.GENERIC_MAX_HEALTH,320).add(EntityAttributes.GENERIC_ARMOR,12).add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE,1).add(EntityAttributes.GENERIC_MOVEMENT_SPEED,.22).add(EntityAttributes.GENERIC_FOLLOW_RANGE,40));
        CommandRegistrationCallback.EVENT.register((d,a,e)->d.register(literal("tecni").then(literal("jefe").requires(s->s.hasPermissionLevel(4)).then(literal("invocar").then(argument("pos",net.minecraft.command.argument.BlockPosArgumentType.blockPos()).executes(c->{
            if(!Boolean.getBoolean("tecni.testServer")){c.getSource().sendError(Text.literal("El jefe está disponible únicamente en PROBAR_JEFE.bat, nunca en el mundo principal."));return 0;}
            var p=net.minecraft.command.argument.BlockPosArgumentType.getLoadedBlockPos(c,"pos");var w=c.getSource().getWorld();
            var old=w.getEntitiesByClass(TrialBoss.class,new Box(p).expand(100),b->true);if(!old.isEmpty()){c.getSource().sendError(Text.literal("Ya existe un jefe en esta arena."));return 0;}
            var boss=new TrialBoss(TYPE,w);boss.home=p;boss.refreshPositionAndAngles(p.getX()+.5,p.getY(),p.getZ()+.5,180,0);w.spawnEntity(boss);return 1;
        }))))));
    }
    @Override protected void initDataTracker(){super.initDataTracker();dataTracker.startTracking(ATTACK,0);dataTracker.startTracking(AGE,0);dataTracker.startTracking(PHASE,1);dataTracker.startTracking(AIM,0F);}
    public int attack(){return dataTracker.get(ATTACK);}public int attackAge(){return dataTracker.get(AGE);}public int phase(){return dataTracker.get(PHASE);}public float aim(){return dataTracker.get(AIM);}
    private List<ServerPlayerEntity> fighters(){return ((ServerWorld)getWorld()).getPlayers(p->p.isAlive()&&!p.isCreative()&&!p.isSpectator()&&p.squaredDistanceTo(this)<1600);}
    @Override public void tick(){
        super.tick();if(getWorld().isClient)return;
        if(!Boolean.getBoolean("tecni.testServer")){discard();return;}
        if(home==null)home=getBlockPos();if(squaredDistanceTo(Vec3d.ofCenter(home))>900)refreshPositionAndAngles(home.getX()+.5,home.getY(),home.getZ()+.5,getYaw(),0);
        var players=fighters();bar.setPercent(getHealth()/getMaxHealth());dataTracker.set(PHASE,getHealth()<112?3:getHealth()<208?2:1);
        for(var p:new ArrayList<>(bar.getPlayers()))if(!players.contains(p))bar.removePlayer(p);for(var p:players)bar.addPlayer(p);
        if(players.isEmpty()){getNavigation().stop();dataTracker.set(ATTACK,0);pause=60;return;}
        var target=players.stream().min(Comparator.comparingDouble(this::squaredDistanceTo)).orElseThrow();
        if(attack()==0){
            getNavigation().startMovingTo(target,1);getLookControl().lookAt(target,25,25);
            if(--pause<=0){getNavigation().stop();setVelocity(0,getVelocity().y,0);origin=getPos();struck.clear();dataTracker.set(ATTACK,next);dataTracker.set(AGE,0);dataTracker.set(AIM,(float)Math.atan2(target.getZ()-getZ(),target.getX()-getX()));next=next%3+1;cue("boss.wake",.8F);}
            return;
        }
        int t=attackAge()+1;dataTracker.set(AGE,t);int windup=phase()==3?26:phase()==2?31:36;
        String hint=attack()==1?"ONDA · salta sobre el anillo":attack()==2?"FRACTURA · sal de la línea":"CRISTALES · esquiva la descarga";
        bar.setName(Text.literal("Custodio · "+hint));
        if(t<windup){if(t%3==0)warning(attack(),t,windup);}
        else if(attack()==1){
            double radius=(t-windup)*.65;
            if(radius<=12){ring(radius,.09,SanctuaryEffects.AZURE);for(var p:players){double dist=p.getPos().distanceTo(origin);if(Math.abs(dist-radius)<1.1&&p.getY()<origin.y+1.15&&struck.add(p.getUuid()))hit(p,10,origin,1);}}
        }else if(attack()==2){
            if(t==windup)cue("boss.strike",1);
            if(t>=windup&&t<windup+14){double a=aim(),distance=(t-windup+1)*1.1;Vec3d tip=origin.add(Math.cos(a)*distance,.2,Math.sin(a)*distance);var w=(ServerWorld)getWorld();w.spawnParticles(SanctuaryEffects.AZURE,tip.x,tip.y+.6,tip.z,14,.3,.7,.3,.06);for(var p:players){Vec3d q=p.getPos().subtract(origin);double along=q.x*Math.cos(a)+q.z*Math.sin(a),side=Math.abs(q.x*-Math.sin(a)+q.z*Math.cos(a));if(along>=0&&along<=distance+.7&&side<1&&Math.abs(q.y)<2&&struck.add(p.getUuid()))hit(p,12,origin,.6);}}
        }else if(t==windup||t==windup+10||(phase()>1&&t==windup+20)){
            cue("boss.strike",.6F);for(int i=-2;i<=2;i++){double a=aim()+i*.23;var shard=new TrialShard(TrialShard.TYPE,getWorld());shard.setOwner(this);shard.refreshPositionAndAngles(getX(),getY()+2,getZ(),0,0);shard.setVelocity(Math.cos(a)*.60,-.035,Math.sin(a)*.60);((ServerWorld)getWorld()).spawnEntity(shard);}
        }
        if(t==windup&&attack()==1)cue("boss.strike",1);
        if(t>windup+38){dataTracker.set(ATTACK,0);pause=phase()==3?26:phase()==2?36:48;bar.setName(Text.literal("Custodio de Pizarra · fase "+phase()));}
    }
    private void warning(int attack,int t,int windup){
        if(attack==1)ring(12,.03,SanctuaryEffects.RUNE);
        else if(attack==2){var w=(ServerWorld)getWorld();for(int i=1;i<16;i++)w.spawnParticles(SanctuaryEffects.RUNE,getX()+Math.cos(aim())*i,getY()+.08,getZ()+Math.sin(aim())*i,1,0,0,0,0);}
        else ((ServerWorld)getWorld()).spawnParticles(SanctuaryEffects.SHARD,getX(),getY()+3.5,getZ(),8,1.2,.25,1.2,.02);
    }
    private void ring(double radius,double y,net.minecraft.particle.ParticleEffect particle){var w=(ServerWorld)getWorld();for(int i=0;i<48;i++){double a=i*Math.PI*2/48;w.spawnParticles(particle,origin.x+Math.cos(a)*radius,origin.y+y,origin.z+Math.sin(a)*radius,1,0,0,0,0);}}
    private void hit(ServerPlayerEntity p,float damage,Vec3d at,double force){if(p.damage(getDamageSources().mobAttack(this),damage))p.takeKnockback(force,at.x-p.getX(),at.z-p.getZ());}
    private void cue(String event,float volume){getWorld().playSound(null,getBlockPos(),Registries.SOUND_EVENT.get(Hardcore.id(event)),SoundCategory.HOSTILE,volume,1);}
    @Override protected void initGoals(){}
    @Override protected boolean isDisallowedInPeaceful(){return false;}
    @Override public void onRemoved(){bar.clearPlayers();super.onRemoved();}
    @Override public void writeCustomDataToNbt(NbtCompound n){super.writeCustomDataToNbt(n);if(home!=null)n.putLong("TrialHome",home.asLong());}
    @Override public void readCustomDataFromNbt(NbtCompound n){super.readCustomDataFromNbt(n);if(n.contains("TrialHome"))home=BlockPos.fromLong(n.getLong("TrialHome"));}
    @Override public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar r){r.add(new AnimationController<>(this,"body",4,s->s.setAndContinue(RawAnimation.begin().thenLoop(attack()==1?"animation.custodio.slam":attack()!=0?"animation.custodio.cast":getVelocity().horizontalLengthSquared()>.0005?"animation.custodio.walk":"animation.custodio.idle"))));}
}
