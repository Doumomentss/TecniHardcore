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

/** No terrain mutations, weather changes, chunk tickets or random scheduling. */
public final class Cataclysms {
    public static final net.minecraft.particle.DefaultParticleType ACID_DROP=net.minecraft.registry.Registry.register(net.minecraft.registry.Registries.PARTICLE_TYPE,Hardcore.id("acid_drop"),net.fabricmc.fabric.api.particle.v1.FabricParticleTypes.simple());
    public static final Identifier CHANNEL=Hardcore.id("cataclysm_v1");
    public static final String[] TYPES={"tornado","terremoto","acida","electrica","meteoritos"};
    public static final class Hazard {
        public final UUID id=UUID.randomUUID();public final ServerWorld world;public final BlockPos center;public final int type,radius,duration;
        public int age;public boolean stopping;public final List<Vec3d> marks=new ArrayList<>();
        Hazard(ServerWorld w,int t,BlockPos c,int r,int seconds){world=w;type=t;center=c;radius=r;duration=seconds*20;}
        public Vec3d position(){double a=age/600.0,orbit=Math.min(radius*.25,24);return Vec3d.ofBottomCenter(center).add(type==0?Math.cos(a)*orbit:0,0,type==0?Math.sin(a)*orbit:0);}
    }
    public static final Map<UUID,Hazard> active=new LinkedHashMap<>();
    public static void init(){
        CommandRegistrationCallback.EVENT.register((d,a,e)->{
            var root=literal("desastre").requires(s->s.hasPermissionLevel(4));
            var start=literal("iniciar");for(int t=0;t<5;t++){
                final int type=t;
                var position=argument("pos",BlockPosArgumentType.blockPos()).executes(c->command(c.getSource(),type,BlockPosArgumentType.getLoadedBlockPos(c,"pos"),96,180));
                position.then(argument("radio",IntegerArgumentType.integer(32,128)).then(argument("segundos",IntegerArgumentType.integer(30,600)).executes(c->command(c.getSource(),type,BlockPosArgumentType.getLoadedBlockPos(c,"pos"),IntegerArgumentType.getInteger(c,"radio"),IntegerArgumentType.getInteger(c,"segundos")))));
                start.then(literal(TYPES[t]).then(position));
            }
            root.then(start).then(literal("listar").executes(c->{for(var h:active.values())c.getSource().sendFeedback(()->Text.literal(h.id+" · "+TYPES[h.type]+" · "+h.center.toShortString()+" · "+Math.max(0,(h.duration-h.age)/20)+" s"),false);return active.size();}))
                .then(literal("detener").then(literal("todos").executes(c->{for(var h:active.values())stop(h);return 1;})).then(argument("id",StringArgumentType.word()).executes(c->{try{var h=active.get(UUID.fromString(StringArgumentType.getString(c,"id")));if(h!=null){stop(h);return 1;}}catch(Exception ignored){}c.getSource().sendError(Text.literal("Desastre inexistente."));return 0;})));
            d.register(literal("tecni").then(root));
        });
        ServerTickEvents.END_SERVER_TICK.register(s->{for(var h:new ArrayList<>(active.values()))tick(h);});
        ServerLifecycleEvents.SERVER_STOPPING.register(s->{for(var h:active.values())send(h,2);active.clear();});
        ServerPlayConnectionEvents.JOIN.register((h,sender,s)->{for(var v:active.values())send(v,0);});
    }
    private static int command(ServerCommandSource s,int type,BlockPos pos,int r,int seconds){String problem=problem(s.getWorld(),type,pos,r,seconds);if(problem!=null){s.sendError(Text.literal(problem));return 0;}var h=start(s.getWorld(),type,pos,r,seconds);s.sendFeedback(()->Text.literal("Desastre preparado: "+h.id+". Comienza en 10 segundos."),true);Hardcore.LOG.warn("ADMIN {} starts {} {} {}",s.getName(),TYPES[type],h.id,pos);return 1;}
    public static String problem(ServerWorld w,int type,BlockPos pos,int r,int seconds){
        if(type<0||type>=TYPES.length)return "Tipo de desastre inválido.";
        if(!ExpansionRules.hazardBounds(r,seconds))return "Radio 32–128 y duración 30–600 s.";
        if(active.values().stream().filter(h->!h.stopping).count()>=(EventDirector.reservesHazard()&&w.getRegistryKey()!=EventDirector.DIMENSION?1:2))return "Ya hay dos desastres activos.";
        if(!w.isChunkLoaded(pos))return "La zona debe estar cargada.";
        if(SpawnProtection.active(w)&&ExpansionRules.plaza(pos.getX(),pos.getZ(),r))return "La zona invade la columna protegida del spawn.";
        if(type==0&&(pos.getY()+200>w.getTopY()-8||pos.getY()<w.getBottomY()))return "El tornado necesita 200 bloques libres bajo el límite del mundo.";
        if(type==0){double orbit=Math.min(r*.25,24);for(int i=0;i<16;i++){double a=i*Math.PI/8;int x=pos.getX()+(int)(Math.cos(a)*orbit),z=pos.getZ()+(int)(Math.sin(a)*orbit);if(!w.isChunkLoaded(new BlockPos(x,pos.getY(),z)))return "Carga toda la trayectoria del tornado antes de iniciarlo.";for(int y=pos.getY()+2;y<pos.getY()+200;y+=2)if(w.getBlockState(new BlockPos(x,y,z)).isSolidBlock(w,new BlockPos(x,y,z)))return "Hay un techo u obstáculo en la trayectoria vertical del tornado.";}}
        for(var h:active.values())if(!h.stopping&&h.world==w&&ExpansionRules.overlaps(Math.hypot(pos.getX()-h.center.getX(),pos.getZ()-h.center.getZ()),r,h.radius))return "La zona se superpone con otro desastre.";
        return null;
    }
    public static Hazard start(ServerWorld w,int type,BlockPos pos,int r,int seconds){String invalid=problem(w,type,pos,r,seconds);if(invalid!=null)throw new IllegalArgumentException(invalid);var h=new Hazard(w,type,pos,r,seconds);active.put(h.id,h);send(h,0);return h;}
    public static void stop(Hazard h){if(!h.stopping){h.stopping=true;h.age=h.duration;h.marks.clear();send(h,1);}}
    public static boolean eligible(ServerPlayerEntity p){return p.isAlive()&&!p.isCreative()&&!p.isSpectator()&&AuthBootstrap.authenticated(p)&&Hardcore.soul(p).lives>0&&!safe(p);}
    private static boolean safe(ServerPlayerEntity p){return SpawnProtection.active(p.getWorld())&&ExpansionRules.plaza(p.getX(),p.getZ(),0);}
    public static boolean roof(ServerWorld w,Vec3d p){int x=MathHelper.floor(p.x),z=MathHelper.floor(p.z);return !w.isChunkLoaded(new BlockPos(x,MathHelper.floor(p.y),z))||w.getTopY(Heightmap.Type.MOTION_BLOCKING,x,z)>p.y+1.9;}
    private static List<ServerPlayerEntity> targets(Hazard h){return h.world.getPlayers(p->eligible(p)&&p.getPos().subtract(Vec3d.ofCenter(h.center)).horizontalLength()<=h.radius);}
    private static void tick(Hazard h){
        ++h.age;if(h.age>h.duration){if(!h.stopping){h.stopping=true;send(h,1);}if(h.age>h.duration+60){send(h,2);active.remove(h.id);}return;}
        if(h.age%10==0)send(h,0);if(h.age<200)return;var players=targets(h);int t=h.age-200;
        if(h.type==0){Vec3d center=h.position();for(var p:players){Vec3d d=center.subtract(p.getPos());double distance=d.horizontalLength();if(distance<48&&p.getY()<center.y+110){var body=p.getVehicle() instanceof CrystalMount m?m:p;Vec3d pull=new Vec3d(d.x,0,d.z).normalize().multiply(.055);var velocity=body.getVelocity();double lift=.12+.4*(1-distance/48);body.setVelocity(velocity.x+pull.x+Math.sin(t*.05)*.015,Math.max(velocity.y,lift),velocity.z+pull.z);body.velocityModified=true;if(distance<10&&t%20==0)harm(p,2,false);}}}
        if(h.type==1&&t%160==40){for(var p:players)if(p.isOnGround()){harm(p,p.isSneaking()?3:6,false);p.addVelocity(0,.18,0);p.velocityModified=true;}}
        if(h.type==2&&t%40==0){for(var p:players)if(!roof(h.world,p.getPos()))harm(p,2,true);}
        int interval=h.type==3?120:80,warning=h.type==3?40:60;
        if(h.type>=3){if(t%interval==0){h.marks.clear();players.stream().limit(8).forEach(p->h.marks.add(p.getPos()));send(h,0);}if(t%interval==warning){for(var mark:h.marks)for(var p:players)if(p.getPos().distanceTo(mark)<(h.type==3?2:4)&&h.world.raycast(new RaycastContext(mark.add(0,8,0),p.getPos().add(0,1,0),RaycastContext.ShapeType.COLLIDER,RaycastContext.FluidHandling.NONE,p)).getType()==net.minecraft.util.hit.HitResult.Type.MISS)harm(p,h.type==3?10:14,true);send(h,3);}if(t%interval==warning+12){h.marks.clear();send(h,0);}}
    }
    private static void harm(ServerPlayerEntity p,float damage,boolean magic){if(p.getVehicle() instanceof CrystalMount m)m.damage(p.getDamageSources().generic(),damage);p.damage(p.getDamageSources().generic(),damage);}
    private static void send(Hazard h,int state){for(var p:h.world.getPlayers(p->p.getPos().subtract(Vec3d.ofCenter(h.center)).horizontalLength()<h.radius+192))if(ServerPlayNetworking.canSend(p,CHANNEL)){
        var b=PacketByteBufs.create();b.writeUuid(h.id);b.writeByte(h.type);b.writeBlockPos(h.center);b.writeVarInt(h.radius);b.writeVarInt(h.age);b.writeVarInt(h.duration);b.writeByte(state);b.writeVarInt(h.marks.size());for(var v:h.marks){b.writeDouble(v.x);b.writeDouble(v.y);b.writeDouble(v.z);}ServerPlayNetworking.send(p,CHANNEL,b);
    }}
}
