package net.tecnihardcore;
import net.fabricmc.fabric.api.object.builder.v1.entity.*;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.entity.*;
import net.minecraft.entity.attribute.*;
import net.minecraft.entity.boss.*;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.*;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.*;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.*;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;
import java.util.*;
import static net.minecraft.server.command.CommandManager.*;

/** All combat decisions, damage, cooldowns and phase changes are server-authoritative. */
public final class TrialBoss extends HostileEntity implements GeoEntity {
 public static final EntityType<TrialBoss> TYPE=Registry.register(Registries.ENTITY_TYPE,Hardcore.id("custodio_pizarra"),FabricEntityTypeBuilder.create(SpawnGroup.MONSTER,TrialBoss::new).dimensions(EntityDimensions.fixed(2.2F,4.3F)).trackRangeBlocks(96).trackedUpdateRate(1).build());
 private static final TrackedData<Integer> ATTACK=DataTracker.registerData(TrialBoss.class,TrackedDataHandlerRegistry.INTEGER),AGE=DataTracker.registerData(TrialBoss.class,TrackedDataHandlerRegistry.INTEGER),PHASE=DataTracker.registerData(TrialBoss.class,TrackedDataHandlerRegistry.INTEGER),TRANSITION=DataTracker.registerData(TrialBoss.class,TrackedDataHandlerRegistry.INTEGER);
 private static final TrackedData<Float> AIM=DataTracker.registerData(TrialBoss.class,TrackedDataHandlerRegistry.FLOAT);
 private static final TrackedData<Integer> MELEE=DataTracker.registerData(TrialBoss.class,TrackedDataHandlerRegistry.INTEGER);
 private static final TrackedData<Integer> DYING=DataTracker.registerData(TrialBoss.class,TrackedDataHandlerRegistry.INTEGER);
 private final java.util.Map<String,Double> contributions=new java.util.LinkedHashMap<>();
 private boolean lootAllowed=true;
 private UUID meleeTarget;
 private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
 private final ServerBossBar bar=new ServerBossBar(Text.literal("Custodio de Pizarra · F1 / 3"),BossBar.Color.BLUE,BossBar.Style.NOTCHED_10);
 private final Set<String> struck=new HashSet<>();
 private final List<Vec3d> marks=new ArrayList<>();
 private BlockPos home;private int pause=20,previous,meleeCooldown;private boolean fighting;
 private BossCombat.Attack cast=BossCombat.plan(1,1);private UUID castId=UUID.randomUUID(),castTarget;private Vec3d origin=Vec3d.ZERO,aimPoint=Vec3d.ZERO;
 public TrialBoss(EntityType<? extends HostileEntity> type,World world){super(type,world);setPersistent();setStepHeight(1.1F);experiencePoints=0;}
 static boolean enabled(){return Boolean.getBoolean("tecni.testServer")||Boolean.getBoolean("tecni.allowTrialBoss");}
 public static void init(){
  FabricDefaultAttributeRegistry.register(TYPE,HostileEntity.createHostileAttributes().add(EntityAttributes.GENERIC_MAX_HEALTH,400).add(EntityAttributes.GENERIC_ARMOR,18).add(EntityAttributes.GENERIC_ARMOR_TOUGHNESS,8).add(EntityAttributes.GENERIC_ATTACK_DAMAGE,12).add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE,1).add(EntityAttributes.GENERIC_MOVEMENT_SPEED,.34).add(EntityAttributes.GENERIC_FOLLOW_RANGE,48));
  CommandRegistrationCallback.EVENT.register((d,a,e)->d.register(literal("tecni").then(literal("jefe").requires(s->s.hasPermissionLevel(4))
   .then(literal("invocar").then(argument("pos",net.minecraft.command.argument.BlockPosArgumentType.blockPos()).executes(c->{
    if(!enabled()){c.getSource().sendError(Text.literal("La invocación del jefe no está habilitada."));return 0;}
    var p=net.minecraft.command.argument.BlockPosArgumentType.getLoadedBlockPos(c,"pos");var w=c.getSource().getWorld();
    if(SpawnProtection.inside(w,p)){c.getSource().sendError(Text.literal("Invócalo fuera de los 32 bloques de radio de la plaza."));return 0;}
    if(!w.getEntitiesByClass(TrialBoss.class,new Box(p).expand(100),b->b.isAlive()).isEmpty()){c.getSource().sendError(Text.literal("Ya existe un jefe cerca."));return 0;}
    var boss=new TrialBoss(TYPE,w);boss.home=p;boss.refreshPositionAndAngles(p.getX()+.5,p.getY(),p.getZ()+.5,180,0);if(!w.spawnEntity(boss))return 0;
    Hardcore.LOG.warn("ADMIN {} summoned Custodio at {}",c.getSource().getName(),p.toShortString());c.getSource().sendFeedback(()->Text.literal("Custodio invocado en "+p.toShortString()),true);return 1;
   })))
   .then(literal("fase").requires(s->Boolean.getBoolean("tecni.testServer")).then(argument("numero",com.mojang.brigadier.arguments.IntegerArgumentType.integer(1,3)).executes(c->{var p=c.getSource().getPlayerOrThrow();var bosses=p.getServerWorld().getEntitiesByClass(TrialBoss.class,p.getBoundingBox().expand(80),b->b.isAlive());if(bosses.isEmpty())return 0;var b=bosses.get(0);b.changePhase(com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(c,"numero"));return 1;})))
  )));
 }
 @Override protected void initDataTracker(){super.initDataTracker();dataTracker.startTracking(ATTACK,0);dataTracker.startTracking(AGE,0);dataTracker.startTracking(PHASE,1);dataTracker.startTracking(TRANSITION,0);dataTracker.startTracking(AIM,0F);dataTracker.startTracking(MELEE,0);dataTracker.startTracking(DYING,0);}
 public int dying(){return dataTracker.get(DYING);}
 public int attack(){return dataTracker.get(ATTACK);}public int attackAge(){return dataTracker.get(AGE);}public int phase(){return dataTracker.get(PHASE);}public float aim(){return dataTracker.get(AIM);}public int transition(){return dataTracker.get(TRANSITION);}
 public static boolean arena(ServerPlayerEntity p){if(!enabled()||SpawnProtection.inside(p.getWorld(),p.getBlockPos()))return false;return !p.getServerWorld().getEntitiesByClass(TrialBoss.class,p.getBoundingBox().expand(48),b->b.isAlive()&&b.fighting&&b.squaredDistanceTo(p)<=48*48).isEmpty();}
 private List<ServerPlayerEntity> fighters(){return ((ServerWorld)getWorld()).getPlayers(p->p.isAlive()&&!p.isCreative()&&!p.isSpectator()&&AuthBootstrap.authenticated(p)&&!SpawnProtection.inside(p.getWorld(),p.getBlockPos())&&p.squaredDistanceTo(this)<=48*48);}
 @Override public void tick(){
  super.tick();if(getWorld().isClient)return;if(!enabled()){discard();return;}if(isDead())return;
  if(home==null)home=getBlockPos();if(squaredDistanceTo(Vec3d.ofCenter(home))>64*64){refreshPositionAndAngles(home.getX()+.5,home.getY(),home.getZ()+.5,getYaw(),0);cancelCast();}
  var players=fighters();fighting=!players.isEmpty();bar.setPercent(MathHelper.clamp(getHealth()/getMaxHealth(),0,1));
  for(var p:new ArrayList<>(bar.getPlayers()))if(!players.contains(p))bar.removePlayer(p);for(var p:players)bar.addPlayer(p);
  if(transition()>0){getNavigation().stop();setVelocity(0,getVelocity().y,0);bar.setName(Text.literal(phase()==3?"TRANSFORMACIÓN · ESPECTRO DEL NÚCLEO":"CORAZA FRACTURADA · FASE 2 / 3"));int elapsed=BossCombat.transitionTicks(phase())-transition();if(transition()%4==0){origin=getPos();ring(Math.min(10,elapsed*.13),.15,SanctuaryEffects.RUNE);particles(getPos().add(0,2,0),SanctuaryEffects.SHARD,12,1.6,1.8,.08);if(elapsed>40)for(int i=0;i<8;i++){double angle=elapsed*.13+i*Math.PI/4;particles(getPos().add(Math.cos(angle)*2.6,1+(elapsed%30)*.09,Math.sin(angle)*2.6),SanctuaryEffects.AZURE,2,.07,.07,.01);}}if(elapsed==BossCombat.transitionTicks(phase())-8)cue("boss.strike",1.1F);dataTracker.set(TRANSITION,transition()-1);return;}
  if(players.isEmpty()){getNavigation().stop();if(attack()!=0)cancelCast();pause=20;bar.setName(Text.literal(title()));return;}
  var target=players.stream().min(Comparator.comparingDouble(this::squaredDistanceTo)).orElseThrow();setTarget(target);getLookControl().lookAt(target,35,35);
  if(meleeCooldown>0)--meleeCooldown;
  if(dataTracker.get(MELEE)>0){int remaining=dataTracker.get(MELEE)-1;dataTracker.set(MELEE,remaining);if(remaining==4){var p=((ServerWorld)getWorld()).getServer().getPlayerManager().getPlayer(meleeTarget);if(p!=null&&players.contains(p)&&squaredDistanceTo(p)<=12.25&&canSee(p)){cue("boss.melee",.8F);hit(p,BossCombat.meleeDamage(phase()),getPos(),.65);particles(p.getPos().add(0,1,0),SanctuaryEffects.SHARD,12,.35,.45,.06);}}}
  if(meleeCooldown==0&&squaredDistanceTo(target)<=12.25&&Math.abs(target.getY()-getY())<2.5&&canSee(target)){
   swingHand(Hand.MAIN_HAND);meleeTarget=target.getUuid();dataTracker.set(MELEE,10);meleeCooldown=BossCombat.meleeCooldown(phase());
  }
  if(attack()==0){bar.setName(Text.literal(title()));getNavigation().startMovingTo(target,1.15+(phase()-1)*.1);if(--pause<=0)begin(target,players);return;}
  int t=attackAge()+1;dataTracker.set(AGE,t);int since=t-cast.windup();bar.setName(Text.literal("F"+phase()+" / 3 · "+BossCombat.name(attack())));
  if(t%10==0)sync(0);if(since<0){if(t%4==0)warning();return;}
  if(since==0)cue(attack()==6||attack()==10?"boss.prison":attack()==3||attack()==9?"boss.volley":"boss.strike",.9F);
  switch(attack()){
   case 1->wave(since,0,BossCombat.damage(1),players);
   case 2->{if(since<14)line(aim(),(since+1)*1.2,BossCombat.damage(2),0,players);}
   case 3->{getNavigation().startMovingTo(target,1.2);if(cast.volley(t))fan(target);}
   case 4->{if(since<14)for(int i=-1;i<=1;i++)line(aim()+i*Math.toRadians(25),(since+1)*1.3,BossCombat.damage(4),0,players);}
   case 5->{if(since%10==0&&since<=20)impact(2,BossCombat.damage(5),since/10,players,false);}
   case 6->{if(since==0)impact(2,BossCombat.damage(6),0,players,true);}
   case 7->{if(since<20)charge(players);}
   case 8->{wave(since,0,BossCombat.damage(8),players);if(since>=12)wave(since-12,1,BossCombat.damage(8),players);}
   case 9->{if(since==0){var locked=((ServerWorld)getWorld()).getServer().getPlayerManager().getPlayer(castTarget);if(locked!=null&&players.contains(locked))homing(locked);}}
   case 10->{if(since==0)impact(2,BossCombat.damage(10),0,players,true);}
   case 11->{if(since==0)for(var mark:marks)for(int i=0;i<4;i++)cross(mark,i*Math.PI/4,BossCombat.damage(11),players);}
   case 12->{if(since%12==0&&since<=36)impact(2.5,BossCombat.damage(12),since/12,players,false);}
  }
  if(t>=cast.finish()){sync(1);dataTracker.set(ATTACK,0);pause=BossCombat.pause(phase());marks.clear();}
 }
 private String title(){return (phase()==3?"Espectro del Núcleo":"Custodio de Pizarra")+" · fase "+phase()+" / 3";}
 private void begin(ServerPlayerEntity target,List<ServerPlayerEntity> players){
  int last=previous;previous=BossCombat.choose(phase(),previous,squaredDistanceTo(target)>144);if(target.getY()>getY()+5)previous=phase()==3?(last==9?12:9):(last==3?5:3);if(previous==10&&players.stream().noneMatch(p->squaredDistanceTo(p)>144))previous=11;cast=BossCombat.plan(phase(),previous);castId=UUID.randomUUID();castTarget=target.getUuid();aimPoint=target.getPos().add(0,1,0);origin=getPos();marks.clear();players.stream().filter(p->previous!=10||squaredDistanceTo(p)>144).sorted(Comparator.comparingDouble(p->previous==10?-squaredDistanceTo(p):squaredDistanceTo(p))).limit(8).forEach(p->marks.add(p.getPos()));struck.clear();dataTracker.set(ATTACK,previous);dataTracker.set(AGE,0);dataTracker.set(AIM,(float)Math.atan2(target.getZ()-getZ(),target.getX()-getX()));getNavigation().stop();setVelocity(0,getVelocity().y,0);cue("boss.warning",.65F);sync(0);
 }
 private void sync(int state){BossNetwork.send(this,castId,attack(),attackAge(),cast.windup(),cast.finish(),state,origin,aim(),marks);}
 private void cancelCast(){if(attack()!=0)sync(2);dataTracker.set(ATTACK,0);dataTracker.set(MELEE,0);marks.clear();struck.clear();for(var shard:((ServerWorld)getWorld()).getEntitiesByClass(TrialShard.class,getBoundingBox().expand(100),s->s.getOwner()==this))shard.discard();}
 private void warning(){switch(attack()){case 1,8->ring(14,.05,SanctuaryEffects.RUNE);case 2,7->runeLine(origin,aim(),attack()==7?16:17);case 4->{for(int i=-1;i<=1;i++)runeLine(origin,aim()+i*Math.toRadians(25),18);}case 11->{for(var m:marks)for(int i=0;i<4;i++)runeLine(m.add(-Math.cos(i*Math.PI/4)*7,0,-Math.sin(i*Math.PI/4)*7),i*Math.PI/4,14);}default->{for(var mark:marks)circle(mark,attack()==12?2.5:2,SanctuaryEffects.RUNE);}}}
 private void wave(int age,int pulse,float damage,List<ServerPlayerEntity> players){double radius=age*.8;if(radius>14)return;ring(radius,.09,SanctuaryEffects.AZURE);for(var p:players){var q=p.getPos().subtract(origin);if(Math.abs(q.horizontalLength()-radius)<1.2&&q.y<1.15&&q.y>-2&&clear(origin.add(0,1,0),p.getPos().add(0,1,0))&&struck.add("wave"+pulse+p.getUuid()))hit(p,damage,origin,1);}}
 private void line(double angle,double distance,float damage,int pulse,List<ServerPlayerEntity> players){Vec3d end=origin.add(Math.cos(angle)*distance,.2,Math.sin(angle)*distance);particles(end,SanctuaryEffects.AZURE,10,.3,.7,.06);for(var p:players){var q=p.getPos().subtract(origin);double along=q.x*Math.cos(angle)+q.z*Math.sin(angle),side=Math.abs(-q.x*Math.sin(angle)+q.z*Math.cos(angle));if(along>=0&&along<=distance+.7&&side<1&&Math.abs(q.y)<2&&clear(origin.add(0,1,0),p.getPos().add(0,1,0))&&struck.add("line"+pulse+p.getUuid()))hit(p,damage,origin,.6);}}
 private void cross(Vec3d mark,double a,float damage,List<ServerPlayerEntity> players){for(int i=-7;i<=7;i++)particles(mark.add(Math.cos(a)*i,.2,Math.sin(a)*i),SanctuaryEffects.AZURE,2,.1,.3,.02);for(var p:players){var q=p.getPos().subtract(mark);if(Math.abs(-q.x*Math.sin(a)+q.z*Math.cos(a))<.7&&q.horizontalLength()<7&&Math.abs(q.y)<2&&clear(mark.add(0,1,0),p.getPos().add(0,1,0))&&struck.add("cross"+p.getUuid()))hit(p,damage,mark,.5);}}
 private void impact(double radius,float damage,int pulse,List<ServerPlayerEntity> players,boolean root){for(var mark:marks){circle(mark,radius,SanctuaryEffects.AZURE);for(int y=0;y<5;y++)particles(mark.add(0,y,0),SanctuaryEffects.SHARD,6,.3,.3,.06);for(var p:players){var q=p.getPos().subtract(mark);if(q.horizontalLength()<radius&&Math.abs(q.y)<2&&clear(mark.add(0,5,0),p.getPos().add(0,1,0))&&clear(getPos().add(0,2,0),p.getPos().add(0,1,0))&&struck.add("impact"+pulse+p.getUuid())){boolean damaged=hit(p,damage,mark,.2);if(root&&damaged)BossRoots.apply(p);}}}}
 private void charge(List<ServerPlayerEntity> players){var delta=new Vec3d(Math.cos(aim())*.8,0,Math.sin(aim())*.8);if(getWorld().isSpaceEmpty(this,getBoundingBox().offset(delta))&&!SpawnProtection.inside(getWorld(),BlockPos.ofFloored(getPos().add(delta))))setPosition(getPos().add(delta));else dataTracker.set(AGE,cast.finish());particles(getPos().add(0,.2,0),SanctuaryEffects.AZURE,8,.8,.3,.03);for(var p:players)if(squaredDistanceTo(p)<12.25&&canSee(p)&&struck.add("charge"+p.getUuid()))hit(p,BossCombat.damage(7),getPos(),1.2);}
 private void fan(ServerPlayerEntity target){double elevation=Math.atan2(aimPoint.y-(origin.y+2),Math.max(1,aimPoint.subtract(origin).horizontalLength()));cue("boss.volley",.7F);for(int i=-2;i<=2;i++){double a=aim()+i*.23;var shard=shard();shard.setVelocity(Math.cos(a)*Math.cos(elevation)*.85,Math.sin(elevation)*.85,Math.sin(a)*Math.cos(elevation)*.85);}}
 private void homing(ServerPlayerEntity target){for(int i=-1;i<=1;i++){var shard=shard();shard.track(target.getUuid());Vec3d direction=target.getPos().add(0,1,0).subtract(shard.getPos()).normalize();double a=Math.atan2(direction.z,direction.x)+i*.25;shard.setVelocity(Math.cos(a)*.7,direction.y*.7,Math.sin(a)*.7);}}
 private TrialShard shard(){var shard=new TrialShard(TrialShard.TYPE,getWorld());shard.setOwner(this);shard.refreshPositionAndAngles(getX(),getY()+2,getZ(),0,0);((ServerWorld)getWorld()).spawnEntity(shard);return shard;}
 private boolean clear(Vec3d from,Vec3d to){return getWorld().raycast(new RaycastContext(from,to,RaycastContext.ShapeType.COLLIDER,RaycastContext.FluidHandling.NONE,this)).getType()==HitResult.Type.MISS;}
 private boolean hit(ServerPlayerEntity p,float damage,Vec3d at,double force){if(p.getVehicle() instanceof CrystalMount mount)mount.damage(getDamageSources().mobAttack(this),damage*.6F);boolean hit=p.damage(getDamageSources().mobAttack(this),damage);if(hit)p.takeKnockback(force,at.x-p.getX(),at.z-p.getZ());return hit;}
 private void runeLine(Vec3d from,double a,int length){for(int i=1;i<=length;i++)particles(from.add(Math.cos(a)*i,.06,Math.sin(a)*i),SanctuaryEffects.RUNE,1,0,0,0);}
 private void ring(double r,double y,net.minecraft.particle.ParticleEffect particle){circle(origin.add(0,y,0),r,particle);}
 private void circle(Vec3d center,double r,net.minecraft.particle.ParticleEffect particle){for(int i=0;i<32;i++){double a=i*Math.PI/16;particles(center.add(Math.cos(a)*r,.06,Math.sin(a)*r),particle,1,0,0,0);}}
 private void particles(Vec3d p,net.minecraft.particle.ParticleEffect type,int count,double horizontal,double vertical,double speed){((ServerWorld)getWorld()).spawnParticles(type,p.x,p.y,p.z,count,horizontal,vertical,horizontal,speed);}
 private void cue(String event,float volume){getWorld().playSound(null,getBlockPos(),Registries.SOUND_EVENT.get(Hardcore.id(event)),SoundCategory.HOSTILE,volume,1);}
 private void changePhase(int n){cancelCast();dataTracker.set(PHASE,n);getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(BossCombat.health(n));setHealth(getMaxHealth());deathTime=0;dataTracker.set(TRANSITION,BossCombat.transitionTicks(n));previous=0;pause=BossCombat.pause(n);meleeCooldown=20;bar.setColor(n==2?BossBar.Color.PURPLE:n==3?BossBar.Color.RED:BossBar.Color.BLUE);bar.setPercent(1);cue(n==3?"boss.transform":"boss.phase",1.2F);Hardcore.LOG.info("Custodio {} entered phase {}",getUuid(),n);}
 @Override protected void initGoals(){goalSelector.add(0,new net.minecraft.entity.ai.goal.SwimGoal(this));}
 @Override public boolean damage(DamageSource source,float amount){if(transition()>0&&!source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY))return false;float before=getHealth();int phaseBefore=phase();boolean hit=super.damage(source,amount);double effective=phase()!=phaseBefore?before:Math.max(0,before-getHealth());if(hit&&effective>0){if(source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY)||source.getAttacker() instanceof ServerPlayerEntity p&&p.isCreative())lootAllowed=false;else if(source.getAttacker() instanceof ServerPlayerEntity p&&AuthBootstrap.authenticated(p)&&!p.isSpectator()&&Hardcore.soul(p).lives>0)contributions.merge(p.getUuidAsString(),effective,Double::sum);}return hit;}
 @Override public void onDeath(DamageSource source){if(!getWorld().isClient&&phase()<3&&!source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY)){changePhase(phase()+1);return;}if(!getWorld().isClient){cancelCast();cue("boss.death",1);}super.onDeath(source);}
 @Override protected boolean isDisallowedInPeaceful(){return false;}
 @Override public void onRemoved(){bar.clearPlayers();if(!getWorld().isClient)cancelCast();super.onRemoved();}
 @Override public void writeCustomDataToNbt(NbtCompound n){super.writeCustomDataToNbt(n);if(home!=null)n.putLong("TrialHome",home.asLong());n.putInt("TrialPhase",phase());n.putInt("TrialTransition",transition());n.putInt("TrialAttack",attack());n.putInt("TrialAttackAge",attackAge());n.putInt("TrialDeathAge",dying());n.putBoolean("TrialLootAllowed",lootAllowed);var damage=new net.minecraft.nbt.NbtList();for(var entry:contributions.entrySet()){var value=new NbtCompound();value.putString("player",entry.getKey());value.putDouble("damage",entry.getValue());damage.add(value);}n.put("TrialContributions",damage);}
 @Override public void readCustomDataFromNbt(NbtCompound n){super.readCustomDataFromNbt(n);lootAllowed=!n.contains("TrialLootAllowed")||n.getBoolean("TrialLootAllowed");for(var value:n.getList("TrialContributions",10)){var record=(NbtCompound)value;contributions.put(record.getString("player"),Math.max(0,record.getDouble("damage")));}if(n.contains("TrialHome"))home=BlockPos.fromLong(n.getLong("TrialHome"));dataTracker.set(PHASE,BossCombat.phase(n.getInt("TrialPhase")));deathTime=Math.max(0,Math.min(120,n.getInt("TrialDeathAge")));dataTracker.set(DYING,deathTime);dataTracker.set(TRANSITION,Math.max(0,Math.min(BossCombat.transitionTicks(phase()),n.getInt("TrialTransition"))));getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(.34);getAttributeInstance(EntityAttributes.GENERIC_ARMOR).setBaseValue(18);getAttributeInstance(EntityAttributes.GENERIC_ARMOR_TOUGHNESS).setBaseValue(8);getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(BossCombat.health(phase()));bar.setColor(phase()==2?BossBar.Color.PURPLE:phase()==3?BossBar.Color.RED:BossBar.Color.BLUE);}
 @Override public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
 @Override protected void updatePostDeath(){
  deathTime++;if(!getWorld().isClient){dataTracker.set(DYING,deathTime);if(lootAllowed&&phase()==3&&!contributions.isEmpty())Rewards.boss(this,contributions,1900);bar.clearPlayers();getNavigation().stop();setVelocity(0,0,0);if(deathTime%4==0){origin=getPos();ring(Math.min(9,deathTime*.1),.15,SanctuaryEffects.RUNE);particles(getPos().add(0,Math.max(.3,3-deathTime*.018),0),SanctuaryEffects.SHARD,14,1.8,1.2,.06);}if(deathTime==90){cue("boss.strike",1);particles(getPos().add(0,2,0),SanctuaryEffects.AZURE,70,2,1.4,.1);}if(deathTime>=120){getWorld().sendEntityStatus(this,EntityStatuses.ADD_DEATH_PARTICLES);remove(RemovalReason.KILLED);}}
 }
 @Override public void registerControllers(AnimatableManager.ControllerRegistrar r){
  r.add(new AnimationController<>(this,"body",2,s->{
   String anim=dying()>0||isDead()?"death":transition()>0?(phase()==3?"transform":"phase_break"):switch(attack()){case 1,8->"slam";case 2,4,11->"fracture";case 3,9->"volley";case 5,12->"meteor";case 6,10->"prison";case 7->"charge";default->getVelocity().horizontalLengthSquared()>.0005?"walk":"idle";};
   boolean action=attack()!=0&&transition()==0&&dying()==0&&!isDead();s.getController().setAnimationSpeed(action?1/(BossCombat.plan(phase(),attack()).windup()/20.0):1);
   var animation=RawAnimation.begin();return s.setAndContinue(anim.equals("idle")||anim.equals("walk")?animation.thenLoop("animation.custodio."+anim):animation.thenPlay("animation.custodio."+anim));
  }));
  r.add(new AnimationController<>(this,"melee",0,s->dataTracker.get(MELEE)>0&&transition()==0&&!isDead()?s.setAndContinue(RawAnimation.begin().thenPlay("animation.custodio.melee")):software.bernie.geckolib.core.object.PlayState.STOP));
 }
}
