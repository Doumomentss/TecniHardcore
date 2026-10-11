package net.tecnihardcore;

import com.google.gson.*;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.event.player.*;
import net.minecraft.block.*;
import net.minecraft.item.*;
import net.minecraft.server.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.world.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import static net.minecraft.server.command.CommandManager.*;

/** Three permanent Custodian dungeons and their crash-safe three-hour cycle. */
public final class BossDungeons {
    private static final long INTERVAL=3L*60*60*1000;
    private static final Gson JSON=new GsonBuilder().setPrettyPrinting().create();
    private static final Arena[] ARENAS={
        new Arena("norte","NORTE",0,-8000,0x55d6ff),
        new Arena("sur","SUR",0,8000,0xff9f45),
        new Arena("oeste","OESTE",-8000,0,0xb982ff)
    };
    private static final ArrayDeque<Job> BUILD=new ArrayDeque<>();
    private static MinecraftServer server;private static State state;private static Path file;
    private static int second,cycleDelay;private static boolean forcedCycle;
    private record Arena(String id,String title,int x,int z,int color){BlockPos center(int y){return new BlockPos(x,y,z);}}
    private record Job(ServerWorld world,BlockPos pos,BlockState block){}
    private static final class State {int schema=1;long nextSpawn;Map<String,Integer> y=new LinkedHashMap<>();Set<String> built=new LinkedHashSet<>();}

    public static void init(){
        ServerLifecycleEvents.SERVER_STARTED.register(BossDungeons::start);
        ServerLifecycleEvents.SERVER_STOPPING.register(s->save());
        ServerLifecycleEvents.SERVER_STOPPED.register(s->{server=null;state=null;file=null;BUILD.clear();});
        ServerTickEvents.END_SERVER_TICK.register(BossDungeons::tick);
        PlayerBlockBreakEvents.BEFORE.register((w,p,pos,bs,be)->p.hasPermissionLevel(2)||!protectedAt(w,pos));
        UseBlockCallback.EVENT.register((p,w,hand,hit)->{
            if(p.hasPermissionLevel(2)||!protectedAt(w,hit.getBlockPos()))return ActionResult.PASS;
            Item item=p.getStackInHand(hand).getItem();
            return item instanceof BlockItem||item instanceof BucketItem||item instanceof FlintAndSteelItem||item instanceof FireChargeItem?ActionResult.FAIL:ActionResult.PASS;
        });
        CommandRegistrationCallback.EVENT.register((d,a,e)->d.register(literal("tecni").then(literal("arenas").requires(s->s.hasPermissionLevel(4))
            .then(literal("estado").executes(c->status(c.getSource())))
            .then(literal("forzar").executes(c->{requestCycle(true);return 1;}))
            .then(literal("construir").executes(c->{prepare(true);return 1;}))
            .then(literal("ir").then(literal("norte").executes(c->teleport(c.getSource().getPlayerOrThrow(),ARENAS[0])))
                .then(literal("sur").executes(c->teleport(c.getSource().getPlayerOrThrow(),ARENAS[1])))
                .then(literal("oeste").executes(c->teleport(c.getSource().getPlayerOrThrow(),ARENAS[2]))))
        )));
    }
    private static void start(MinecraftServer s){
        server=s;file=s.getSavePath(WorldSavePath.ROOT).resolve("tecnihardcore-boss-arenas.json");
        try{state=Files.exists(file)?JSON.fromJson(Files.readString(file,StandardCharsets.UTF_8),State.class):new State();if(state==null||state.schema!=1||state.y==null||state.built==null)throw new IllegalStateException("estado inválido");}
        catch(Exception ex){throw new IllegalStateException("No se puede leer el estado de las arenas; se evita reconstruirlas",ex);}
        prepare(false);
    }
    private static void prepare(boolean force){
        if(server==null||!BUILD.isEmpty())return;ServerWorld w=server.getOverworld();
        for(Arena arena:ARENAS){
            if(state.built.contains(arena.id)&&!force)continue;
            int y=terrainLevel(w,arena);
            state.y.put(arena.id,y);queue(w,arena,y);state.built.remove(arena.id);
        }
        if(BUILD.isEmpty()){if(state.nextSpawn==0){state.nextSpawn=System.currentTimeMillis();save();}return;}
        server.getPlayerManager().broadcast(Text.literal("§5[TECNIHARDCORE] §dLas tres mazmorras del Custodio se están materializando..."),false);
    }
    private static int terrainLevel(ServerWorld w,Arena arena){
        int[] heights=new int[8];int[][] offsets={{60,0},{-60,0},{0,60},{0,-60},{48,48},{48,-48},{-48,48},{-48,-48}};
        for(int i=0;i<offsets.length;i++){int x=arena.x+offsets[i][0],z=arena.z+offsets[i][1];w.getChunk(new BlockPos(x,64,z));heights[i]=w.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,x,z);}
        Arrays.sort(heights);return MathHelper.clamp((heights[3]+heights[4])/2,62,180);
    }
    private static void tick(MinecraftServer s){
        if(s.getTicks()%20==0&&state!=null&&state.built.size()==3){for(Arena a:ARENAS){int y=state.y.getOrDefault(a.id,96);var w=s.getOverworld();var bosses=w.getEntitiesByClass(TrialBoss.class,new Box(a.center(y)).expand(70),b->b.isAlive());for(int i=1;i<bosses.size();i++)bosses.get(i).discard();for(var e:w.getOtherEntities(null,new Box(a.center(y)).expand(46),e->!(e instanceof ServerPlayerEntity)&&!(e instanceof TrialBoss))){e.discard();}}}
        if(!BUILD.isEmpty()){
            for(int i=0;i<5000&&!BUILD.isEmpty();i++){Job j=BUILD.removeFirst();j.world.getChunk(j.pos);j.world.setBlockState(j.pos,j.block,2);}
            if(BUILD.isEmpty()){
                for(Arena a:ARENAS)state.built.add(a.id);if(state.nextSpawn==0)state.nextSpawn=System.currentTimeMillis();save();
                s.getPlayerManager().broadcast(Text.literal("§5[TECNIHARDCORE] §aLas mazmorras Norte, Sur y Oeste están listas."),false);
            }return;
        }
        if(cycleDelay>0&&--cycleDelay==0){spawnCycle(forcedCycle);forcedCycle=false;return;}
        if(++second<20)return;second=0;if(state==null||state.built.size()<3)return;
        if(state.nextSpawn==0){state.nextSpawn=System.currentTimeMillis();save();}
        if(System.currentTimeMillis()>=state.nextSpawn)requestCycle(false);
    }
    private static void requestCycle(boolean forced){
        if(server==null||state==null||state.built.size()<3){if(server!=null)server.getPlayerManager().broadcast(Text.literal("§cLas mazmorras todavía se están construyendo."),false);return;}
        if(cycleDelay>0)return;ServerWorld w=server.getOverworld();for(Arena a:ARENAS){int y=state.y.getOrDefault(a.id,96);w.getChunk(a.center(y));}
        forcedCycle=forced;cycleDelay=100;
    }
    private static void spawnCycle(boolean forced){
        if(server==null||state==null||state.built.size()<3){if(server!=null)server.getPlayerManager().broadcast(Text.literal("§cLas mazmorras todavía se están construyendo."),false);return;}
        ServerWorld w=server.getOverworld();int spawned=0,alive=0;
        for(Arena a:ARENAS){int y=state.y.getOrDefault(a.id,96);BlockPos p=a.center(y+2);w.getChunk(p);
            var existing=w.getEntitiesByClass(TrialBoss.class,new Box(p).expand(70),b->b.isAlive());
            if(!existing.isEmpty()){alive++;continue;}
            TrialBoss boss=new TrialBoss(TrialBoss.TYPE,w);boss.refreshPositionAndAngles(p.getX()+.5,p.getY(),p.getZ()+.5,180,0);if(w.spawnEntity(boss))spawned++;
        }
        state.nextSpawn=System.currentTimeMillis()+INTERVAL;save();
        String msg=spawned>0?"§4⚔ LOS CUSTODIOS HAN DESPERTADO §c· Mazmorras NORTE (0 -8000), SUR (0 8000) y OESTE (-8000 0) · Próximo ciclo en 3 horas":"§6Ciclo del Custodio: los tres guardianes anteriores siguen vivos.";
        server.getPlayerManager().broadcast(Text.literal("§5[TECNIHARDCORE] "+msg),false);
        for(ServerPlayerEntity p:server.getPlayerManager().getPlayerList())if(spawned>0)p.sendMessage(Text.literal("§dDerrota al Custodio y causa al menos 5% del daño para participar por una montura."),false);
        Hardcore.LOG.warn("Boss dungeon cycle: spawned={}, alreadyAlive={}, forced={}",spawned,alive,forced);
    }
    private static int status(net.minecraft.server.command.ServerCommandSource src){
        long left=Math.max(0,state.nextSpawn-System.currentTimeMillis());src.sendFeedback(()->Text.literal("Arenas: "+state.built.size()+"/3 · construcción pendiente: "+BUILD.size()+" bloques · próximo ciclo: "+format(left)),false);
        for(Arena a:ARENAS){int y=state.y.getOrDefault(a.id,0);src.sendFeedback(()->Text.literal(a.title+": "+a.x+" "+y+" "+a.z),false);}return 1;
    }
    private static int teleport(ServerPlayerEntity p,Arena a){int y=state.y.getOrDefault(a.id,96);p.teleport(server.getOverworld(),a.x+.5,y+3,a.z+31.5,180,0);return 1;}
    private static String format(long ms){long total=ms/1000;return String.format("%02d:%02d:%02d",total/3600,total/60%60,total%60);}
    private static boolean protectedAt(World w,BlockPos p){if(!(w instanceof ServerWorld)||w.getRegistryKey()!=World.OVERWORLD||state==null)return false;for(Arena a:ARENAS){long dx=p.getX()-a.x,dz=p.getZ()-a.z;if(dx*dx+dz*dz<=46L*46&&p.getY()>=state.y.getOrDefault(a.id,96)-3&&p.getY()<=state.y.getOrDefault(a.id,96)+26)return true;}return false;}
    static boolean inside(ServerPlayerEntity p){if(state==null||p.getWorld().getRegistryKey()!=World.OVERWORLD)return false;for(Arena a:ARENAS){int y=state.y.getOrDefault(a.id,96);long dx=Math.round(p.getX())-a.x,dz=Math.round(p.getZ())-a.z;if(dx*dx+dz*dz<=42L*42&&p.getY()>=y-2&&p.getY()<=Math.min(y+18,84))return true;}return false;}
    private static void put(ServerWorld w,BlockPos p,Block block){BUILD.addLast(new Job(w,p,block.getDefaultState()));}
    private static void queue(ServerWorld w,Arena a,int y){
        BlockPos c=a.center(y);int r=45;
        for(int x=-r;x<=r;x++)for(int z=-r;z<=r;z++){double d=Math.sqrt(x*x+z*z);if(d>r)continue;BlockPos p=c.add(x,0,z);
            for(int yy=-2;yy<=20;yy++)put(w,p.up(yy),yy<0?Blocks.DEEPSLATE_BRICKS:Blocks.AIR);
            Block floor=((x+z)&7)==0?Blocks.POLISHED_BASALT:d<8?Blocks.CHISELED_POLISHED_BLACKSTONE:((x/5+z/5)&1)==0?Blocks.DEEPSLATE_TILES:Blocks.POLISHED_BLACKSTONE_BRICKS;put(w,p.down(),floor);
            if(d>=39&&d<=42){for(int yy=0;yy<=7;yy++)put(w,p.up(yy),yy==7?Blocks.POLISHED_BLACKSTONE_BRICK_WALL:Blocks.POLISHED_BLACKSTONE_BRICKS);}
        }
        // Four monumental towers, arches, lighting and a central combat dais.
        for(int[] q:new int[][]{{28,28},{28,-28},{-28,28},{-28,-28}})for(int x=-5;x<=5;x++)for(int z=-5;z<=5;z++){double d=Math.sqrt(x*x+z*z);if(d<=5.5)for(int yy=0;yy<=15;yy++)if(d>=3.7||yy==0||yy==15)put(w,c.add(q[0]+x,yy,q[1]+z),yy==15?Blocks.GILDED_BLACKSTONE:Blocks.DEEPSLATE_BRICKS);}
        for(int x=-7;x<=7;x++)for(int z=-7;z<=7;z++){double d=Math.sqrt(x*x+z*z);if(d<=7){put(w,c.add(x,0,z),d>5?Blocks.GILDED_BLACKSTONE:Blocks.POLISHED_BLACKSTONE);if(d>5)put(w,c.add(x,1,z),Blocks.POLISHED_BLACKSTONE_BRICK_WALL);}}
        for(int side=-1;side<=1;side+=2)for(int width=-4;width<=4;width++)for(int yy=0;yy<=6;yy++){put(w,c.add(width,yy,side*40),Blocks.AIR);put(w,c.add(side*40,yy,width),Blocks.AIR);}
        for(int i=0;i<16;i++){double angle=i*Math.PI/8;int x=(int)Math.round(Math.cos(angle)*24),z=(int)Math.round(Math.sin(angle)*24);put(w,c.add(x,0,z),Blocks.CHISELED_POLISHED_BLACKSTONE);put(w,c.add(x,1,z),Blocks.SOUL_LANTERN);}
    }
    private static synchronized void save(){if(state==null||file==null)return;try{Path tmp=file.resolveSibling(file.getFileName()+".tmp");Files.writeString(tmp,JSON.toJson(state),StandardCharsets.UTF_8);Files.move(tmp,file,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}catch(Exception e){throw new IllegalStateException("No se pudo guardar el ciclo de arenas",e);}}
}
