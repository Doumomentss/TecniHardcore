package net.tecnihardcore;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.*;
import net.minecraft.world.*;
import net.minecraft.text.Text;
import java.util.*;
import static net.minecraft.server.command.CommandManager.*;
import com.mojang.brigadier.arguments.*;

/** Operator-only weather. Destruction is opt-in; no chunk tickets or random scheduling. */
public final class Cataclysms {
    public static final net.minecraft.particle.DefaultParticleType ACID_DROP=net.minecraft.registry.Registry.register(net.minecraft.registry.Registries.PARTICLE_TYPE,Hardcore.id("acid_drop"),net.fabricmc.fabric.api.particle.v1.FabricParticleTypes.simple());
    public static final Identifier CHANNEL=Hardcore.id("cataclysm_v3");
    public static final String[] TYPES={"tornado","terremoto","acida","electrica","meteoritos"};
    public static final class Hazard {
        public final UUID id=UUID.randomUUID();public final ServerWorld world;public final BlockPos center;public final int type,radius,duration,width,destruction;
        public int age,destroyed,fractureCursor;public boolean stopping;public double crownY;private Vec3d position;public final List<Vec3d> marks=new ArrayList<>();public final Deque<BlockPos> pendingBlocks=new ArrayDeque<>();
        Hazard(ServerWorld w,int t,BlockPos c,int r,int seconds,int width,int destruction){world=w;type=t;double floor=StormTerrain.floor(w,c.getX(),c.getZ(),c.getY());center=new BlockPos(c.getX(),(int)Math.floor(floor),c.getZ());radius=t==0?WeatherRules.envelope(r,width):r;duration=seconds*20;this.width=width;this.destruction=destruction;position=Vec3d.ofBottomCenter(center);crownY=floor+200;move();}
        public Vec3d position(){return position;}
        private void move(){
            if(type!=0)return;
            double a=WeatherRules.angle(age),orbit=WeatherRules.orbit(radius),x=center.getX()+.5+Math.cos(a)*orbit,z=center.getZ()+.5+Math.sin(a)*orbit;
            var probe=new BlockPos((int)Math.floor(x),world.getBottomY(),(int)Math.floor(z));
            if(!world.isChunkLoaded(probe)||!world.getWorldBorder().contains(probe))return;
            double floor=age%4==0?StormTerrain.footprint(world,x,z,position.y,width):position.y;
            position=new Vec3d(x,floor,z);crownY=WeatherRules.crown(crownY,floor);
        }
    }
    public static final Map<UUID,Hazard> active=new LinkedHashMap<>();
    public static void init(){
        CommandRegistrationCallback.EVENT.register((d,a,e)->{
            var root=literal("desastre").requires(s->s.hasPermissionLevel(4));
            var start=literal("iniciar");for(int t=0;t<5;t++){
                final int type=t;
                var position=argument("pos",BlockPosArgumentType.blockPos()).executes(c->command(c.getSource(),type,BlockPosArgumentType.getBlockPos(c,"pos"),WeatherRules.DEFAULT_RADIUS,180,120,0));
                var seconds=argument("segundos",IntegerArgumentType.integer(30,600)).executes(c->command(c.getSource(),type,BlockPosArgumentType.getBlockPos(c,"pos"),IntegerArgumentType.getInteger(c,"radio"),IntegerArgumentType.getInteger(c,"segundos"),120,0));
                if(type<=1||type==4)seconds.then(literal("destruccion").then(argument("nivel",IntegerArgumentType.integer(0,WeatherRules.maxDestruction(type))).executes(c->command(c.getSource(),type,BlockPosArgumentType.getBlockPos(c,"pos"),IntegerArgumentType.getInteger(c,"radio"),IntegerArgumentType.getInteger(c,"segundos"),120,IntegerArgumentType.getInteger(c,"nivel")))));
                if(type==0)seconds.then(literal("ancho").then(argument("ancho",IntegerArgumentType.integer(40,600)).executes(c->command(c.getSource(),type,BlockPosArgumentType.getBlockPos(c,"pos"),IntegerArgumentType.getInteger(c,"radio"),IntegerArgumentType.getInteger(c,"segundos"),IntegerArgumentType.getInteger(c,"ancho"),0)).then(literal("destruccion").then(argument("nivel",IntegerArgumentType.integer(0,WeatherRules.maxDestruction(type))).executes(c->command(c.getSource(),type,BlockPosArgumentType.getBlockPos(c,"pos"),IntegerArgumentType.getInteger(c,"radio"),IntegerArgumentType.getInteger(c,"segundos"),IntegerArgumentType.getInteger(c,"ancho"),IntegerArgumentType.getInteger(c,"nivel")))))));
                position.then(argument("radio",IntegerArgumentType.integer(32,WeatherRules.MAX_RADIUS)).then(seconds));
                start.then(literal(TYPES[t]).then(position));
            }
            root.then(start).then(literal("listar").executes(c->{for(var h:active.values())c.getSource().sendFeedback(()->Text.literal(h.id+" · "+TYPES[h.type]+" · "+h.center.toShortString()+" · "+Math.max(0,(h.duration-h.age)/20)+" s"),false);return active.size();}))
                .then(literal("detener").then(literal("todos").executes(c->{for(var h:active.values())stop(h);return 1;})).then(argument("id",StringArgumentType.word()).executes(c->{try{var h=active.get(UUID.fromString(StringArgumentType.getString(c,"id")));if(h!=null){stop(h);return 1;}}catch(Exception ignored){}c.getSource().sendError(Text.literal("Desastre inexistente."));return 0;})));
            d.register(literal("tecni").then(root));
        });
        ServerTickEvents.END_SERVER_TICK.register(s->{for(var h:new ArrayList<>(active.values()))tick(h);});
        ServerLifecycleEvents.SERVER_STOPPING.register(s->{for(var h:active.values())send(h,2);active.clear();StormRubble.clear();});
        ServerPlayConnectionEvents.JOIN.register((h,sender,s)->{for(var v:active.values())send(v,0);});
    }
    private static int command(ServerCommandSource s,int type,BlockPos pos,int r,int seconds,int width,int destruction){if(!WeatherRules.settings(type,r,seconds,width,destruction)){s.sendError(Text.literal("Configuración inválida: radio 32–512, ancho 40–600 y destrucción 0–20 en tornado, 0–4 en terremoto/meteoritos."));return 0;}int area=type==0?WeatherRules.envelope(r,width):r;String problem=problem(s.getWorld(),type,pos,area,seconds);if(problem!=null){s.sendError(Text.literal(problem));return 0;}var h=start(s.getWorld(),type,pos,r,seconds,width,destruction);s.sendFeedback(()->Text.literal("Desastre preparado: "+h.id+". Radio "+h.radius+" · ancho "+width+" · destrucción "+destruction+". Comienza en 10 segundos."),true);Hardcore.LOG.warn("ADMIN {} starts {} {} {} width {} destruction {}",s.getName(),TYPES[type],h.id,pos,width,destruction);return 1;}
    public static String problem(ServerWorld w,int type,BlockPos pos,int r,int seconds){
        if(type<0||type>=TYPES.length)return "Tipo de desastre inválido.";
        if(!ExpansionRules.hazardBounds(r,seconds))return "Radio 32–512 y duración 30–600 s.";
        if(active.values().stream().filter(h->!h.stopping).count()>=(EventDirector.reservesHazard()&&w.getRegistryKey()!=EventDirector.DIMENSION?1:2))return "Ya hay dos desastres activos.";
        if(!w.isChunkLoaded(pos))return "La zona debe estar cargada.";
        if(SpawnProtection.active(w)&&ExpansionRules.plaza(pos.getX(),pos.getZ(),r))return "La zona invade la columna protegida del spawn.";
        if(!w.getWorldBorder().contains(pos))return "La zona está fuera del borde del mundo.";
        for(var h:active.values())if(!h.stopping&&h.world==w&&ExpansionRules.overlaps(Math.hypot(pos.getX()-h.center.getX(),pos.getZ()-h.center.getZ()),r,h.radius))return "La zona se superpone con otro desastre.";
        return null;
    }
    public static Hazard start(ServerWorld w,int type,BlockPos pos,int r,int seconds){return start(w,type,pos,r,seconds,120,0);}
    public static Hazard start(ServerWorld w,int type,BlockPos pos,int r,int seconds,int width,int destruction){int area=type==0?WeatherRules.envelope(r,width):r;String invalid=WeatherRules.settings(type,r,seconds,width,destruction)?problem(w,type,pos,area,seconds):"Invalid storm settings";if(invalid!=null)throw new IllegalArgumentException(invalid);var h=new Hazard(w,type,pos,r,seconds,width,destruction);active.put(h.id,h);send(h,0);return h;}
    public static void stop(Hazard h){if(!h.stopping){h.stopping=true;h.age=h.duration;h.marks.clear();h.pendingBlocks.clear();StormRubble.forget(h.id);send(h,1);}}
    public static boolean eligible(ServerPlayerEntity p){return p.isAlive()&&!p.isCreative()&&!p.isSpectator()&&AuthBootstrap.authenticated(p)&&Hardcore.soul(p).lives>0&&!safe(p);}
    private static boolean safe(ServerPlayerEntity p){return SpawnProtection.active(p.getWorld())&&ExpansionRules.plaza(p.getX(),p.getZ(),0);}
    public static boolean roof(ServerWorld w,Vec3d p){int x=MathHelper.floor(p.x),z=MathHelper.floor(p.z);return !w.isChunkLoaded(new BlockPos(x,MathHelper.floor(p.y),z))||w.getTopY(Heightmap.Type.MOTION_BLOCKING,x,z)>p.y+1.9;}
    private static List<ServerPlayerEntity> targets(Hazard h){return h.world.getPlayers(p->eligible(p)&&p.getPos().subtract(Vec3d.ofCenter(h.center)).horizontalLength()<=h.radius);}
    private static void tick(Hazard h){
        ++h.age;if(h.age>h.duration){if(!h.stopping){h.stopping=true;send(h,1);}if(h.age>h.duration+60){send(h,2);StormRubble.forget(h.id);active.remove(h.id);}return;}
        h.move();
        if(h.age%10==0)send(h,0);if(h.age<200)return;StormDestruction.tick(h);var players=targets(h);int t=h.age-200;
        if(h.type==0){Vec3d center=h.position();double reach=WeatherRules.tornadoReach(h.radius,h.width),force=WeatherRules.tornadoForce(h.radius,h.width);for(var p:players){Vec3d d=center.subtract(p.getPos());double distance=d.horizontalLength();if(distance<reach&&p.getY()>=center.y-4&&p.getY()<h.crownY){var body=p.getVehicle() instanceof CrystalMount m?m:p;Vec3d pull=new Vec3d(d.x,0,d.z).normalize().multiply(.055*force*(.2+.8*(1-distance/reach)));var velocity=body.getVelocity();double lift=Math.min(1.2,.12+.4*force*(1-distance/reach));Vec3d horizontal=new Vec3d(velocity.x+pull.x+Math.sin(t*.05)*.015*force,0,velocity.z+pull.z);double cap=Math.min(2,1.1+.3*force);if(horizontal.length()>cap)horizontal=horizontal.normalize().multiply(cap);body.setVelocity(horizontal.x,Math.max(velocity.y,lift),horizontal.z);body.velocityModified=true;if(distance<10*force&&t%20==0)harm(p,2,false);}}}
        if(h.type==1&&t%160==40){for(var p:players)if(p.isOnGround()){harm(p,p.isSneaking()?3:6,false);p.addVelocity(0,.18,0);p.velocityModified=true;}}
        if(h.type==2&&t%40==0){for(var p:players)if(!roof(h.world,p.getPos()))harm(p,2,true);}
        int interval=h.type==3?120:80,warning=h.type==3?40:60;
        if(h.type>=3){
            if(t%interval==0){h.marks.clear();
                for(var p:players.stream().limit(8).toList()){if(h.type==3)h.marks.add(p.getPos());else addMark(h,p.getX(),p.getZ());}
                if(h.type==4&&h.marks.isEmpty()){
                    var viewers=h.world.getPlayers(p->p.getPos().subtract(Vec3d.ofCenter(h.center)).horizontalLength()<h.radius);
                    for(var p:viewers.stream().limit(3).toList())addMark(h,p.getX(),p.getZ());
                }
                if(h.type==4){addMark(h,h.center.getX()+.5,h.center.getZ()+.5);for(int i=0;i<3&&h.marks.size()<4;i++){double a=h.world.random.nextDouble()*Math.PI*2,d=8+h.world.random.nextDouble()*Math.min(48,h.radius*.6);addMark(h,h.center.getX()+.5+Math.cos(a)*d,h.center.getZ()+.5+Math.sin(a)*d);}}
                send(h,0);
            }
            if(t%interval==warning){
                var struck=new HashSet<UUID>();
                for(var mark:h.marks){
                    for(var p:players)if(!struck.contains(p.getUuid())&&p.getPos().distanceTo(mark)<(h.type==3?2:4)&&h.world.raycast(new RaycastContext(mark.add(0,.7,0),p.getPos().add(0,1,0),RaycastContext.ShapeType.COLLIDER,RaycastContext.FluidHandling.NONE,p)).getType()==net.minecraft.util.hit.HitResult.Type.MISS){harm(p,h.type==3?10:14,true);struck.add(p.getUuid());}
                    if(h.type==4)StormDestruction.impact(h,mark);
                }
                send(h,3);
            }
            if(t%interval==warning+18){h.marks.clear();send(h,0);}
        }
    }
    private static void addMark(Hazard h,double x,double z){
        if(h.marks.size()>=8)return;var pos=new BlockPos((int)Math.floor(x),h.world.getBottomY(),(int)Math.floor(z));
        if(!h.world.isChunkLoaded(pos)||SpawnProtection.inside(h.world,pos)||!h.world.getWorldBorder().contains(pos))return;
        int y=h.world.getTopY(Heightmap.Type.MOTION_BLOCKING,pos.getX(),(int)Math.floor(z));
        var mark=new Vec3d(x,y+.05,z);if(h.marks.stream().noneMatch(v->v.squaredDistanceTo(mark)<9))h.marks.add(mark);
    }
    private static void harm(ServerPlayerEntity p,float damage,boolean magic){if(p.getVehicle() instanceof CrystalMount m)m.damage(p.getDamageSources().generic(),damage);p.damage(p.getDamageSources().generic(),damage);}
    private static void send(Hazard h,int state){for(var p:h.world.getPlayers(p->p.getPos().subtract(Vec3d.ofCenter(h.center)).horizontalLength()<h.radius+192))if(ServerPlayNetworking.canSend(p,CHANNEL)){
        var b=PacketByteBufs.create();b.writeUuid(h.id);b.writeByte(h.type);b.writeBlockPos(h.center);b.writeVarInt(h.radius);b.writeVarInt(h.age);b.writeVarInt(h.duration);b.writeVarInt(h.width);b.writeByte(h.destruction);b.writeByte(state);Vec3d motion=h.position();b.writeDouble(motion.x);b.writeDouble(motion.y);b.writeDouble(motion.z);b.writeDouble(h.crownY);b.writeVarInt(h.marks.size());for(var v:h.marks){b.writeDouble(v.x);b.writeDouble(v.y);b.writeDouble(v.z);}ServerPlayNetworking.send(p,CHANNEL,b);
    }}
}
