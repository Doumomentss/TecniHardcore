package net.tecnihardcore;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/** Two-minute rolling segments backed by the pinned ServerReplay API, not fake players. */
final class DeathReplays {
    private static final Map<UUID,Session> active=new HashMap<>();
    private static final java.util.concurrent.ConcurrentLinkedQueue<Runnable> completions=new java.util.concurrent.ConcurrentLinkedQueue<>();
    private static final Map<UUID,Long> retryAt=new HashMap<>();
    private static Class<?> manager;
    private static Path root;
    private static boolean ready;
    private static String failure="";
    private static MinecraftServer server;
    private static final long SEGMENT_MS=120_000, POST_MS=15_000;
    private static class Session {Object recorder;String current,previous;long started,finishDeathAt;}
    static void start(MinecraftServer s) {
        server=s;root=s.getRunDirectory().toPath().resolve("recordings/tecni-moderacion").toAbsolutePath().normalize();
        active.clear();retryAt.clear();completions.clear();ready=false;failure="";
        try {
            if(!FabricLoader.getInstance().isModLoaded("server-replay"))throw new IllegalStateException("Falta ServerReplay 1.2.2");
            manager=Class.forName("me.senseiwells.replay.player.PlayerRecorders");
            manager.getMethod("create",ServerPlayerEntity.class);manager.getMethod("getByUUID",UUID.class);
            Files.createDirectories(root);ready=true;
        } catch(Exception e){failure=e.getMessage();Hardcore.LOG.error("Death replay recording unavailable: {}",failure);}
        repair();
    }
    private static Object call(Object object,String method,Class<?>[] types,Object... args) throws Exception {return object.getClass().getMethod(method,types).invoke(object,args);}
    private static boolean stopped(Object recorder) throws Exception {return (boolean)call(recorder,"getStopped",new Class<?>[]{});}
    private static Session begin(ServerPlayerEntity p,String previous) throws Exception {
        if(Files.getFileStore(root).getUsableSpace()<2L*1024*1024*1024)throw new IllegalStateException("Menos de 2 GB libres; grabación pausada sin borrar evidencia");
        if(manager.getMethod("getByUUID",UUID.class).invoke(null,p.getUuid())!=null)throw new IllegalStateException("Ya existe otro grabador para este jugador");
        Object recorder=manager.getMethod("create",ServerPlayerEntity.class).invoke(null,p);
        Session session=new Session();session.recorder=recorder;session.started=System.currentTimeMillis();session.previous=previous;
        Path base=((Path)call(recorder,"getLocation",new Class<?>[]{})).toAbsolutePath().normalize();
        Path finalFile=Path.of(base.toString()+".mcpr");
        if(!finalFile.startsWith(root)) {call(recorder,"stop",new Class<?>[]{boolean.class},false);throw new IllegalStateException("Configura player_recording_path como recordings/tecni-moderacion");}
        session.current=root.relativize(finalFile).toString().replace('\\','/');
        if(!ModerationStore.validPath(session.current))throw new IllegalStateException("Ruta de replay inválida");
        var record=new ModerationStore.Segment();record.path=session.current;record.player=p.getUuidAsString();record.started=session.started;
        Moderation.store.data.segments.put(record.path,record);Moderation.store.save();
        if(!(boolean)call(recorder,"start",new Class<?>[]{boolean.class},false)){call(recorder,"stop",new Class<?>[]{boolean.class},false);throw new IllegalStateException("El grabador no pudo inicializar los chunks");}
        active.put(p.getUuid(),session);return session;
    }
    static void tick() {
        if(Moderation.store==null)return;Runnable completion;while((completion=completions.poll())!=null)completion.run();long now=System.currentTimeMillis();
        if(ready)for(var p:server.getPlayerManager().getPlayerList()) {
            if(!AuthBootstrap.authenticated(p))continue;
            var session=active.get(p.getUuid());
            if(session==null) {
                if(!p.isSpectator()&&now>=retryAt.getOrDefault(p.getUuid(),0L))try{begin(p,null);}catch(Exception e){retryAt.put(p.getUuid(),now+30_000);warn(p,e);}
                continue;
            }
            try {if(stopped(session.recorder)||session.finishDeathAt>0&&now>=session.finishDeathAt||session.finishDeathAt==0&&now-session.started>=SEGMENT_MS)rotate(p,session);}catch(Exception e){warn(p,e);}
        }
        if(server.getTicks()%100==0)repair();
    }
    private static void warn(ServerPlayerEntity p,Exception e){failure=e.getCause()!=null?e.getCause().toString():e.toString();Hardcore.LOG.error("Replay for {} unavailable: {}",p.getUuid(),failure);for(var op:server.getPlayerManager().getPlayerList())if(server.getPlayerManager().isOperator(op.getGameProfile()))op.sendMessage(Text.literal("[Moderación] No se está grabando a "+p.getName().getString()+": "+failure),false);}
    static void death(ServerPlayerEntity p,DamageSource damage,UUID killer,String killerName) {
        var clip=new ModerationStore.Clip();clip.id=Integer.toString(Moderation.store.data.nextId++);clip.player=p.getUuidAsString();clip.name=p.getName().getString();clip.killer=killer==null?"":killer.toString();clip.killerName=killerName;clip.damage=damage.getName();clip.time=System.currentTimeMillis();clip.dimension=p.getWorld().getRegistryKey().getValue().toString();clip.x=p.getX();clip.y=p.getY();clip.z=p.getZ();
        var session=active.get(p.getUuid());
        if(session!=null)try {
            clip.markerMillis=((Number)call(session.recorder,"getTimestamp",new Class<?>[]{})).longValue();
            if(session.previous!=null)retain(clip,session.previous);
            retain(clip,session.current);session.finishDeathAt=System.currentTimeMillis()+POST_MS;
            try {call(session.recorder,"addMarker",new Class<?>[]{String.class},"MUERTE-"+clip.id);}catch(Exception markerError){Hardcore.LOG.warn("Death replay kept without embedded marker: {}",markerError.toString());}
        }catch(Exception e){clip.error=e.toString();clip.status="error";}
        else {clip.status="sin_grabacion";clip.error=failure.isEmpty()?"Muerte antes de iniciar la grabación":failure;}
        Moderation.store.data.clips.put(clip.id,clip);Moderation.store.save();
        Hardcore.LOG.info("MODERATION death #{} player={} killer={} replay={}",clip.id,clip.player,clip.killer,clip.status);
        for(var op:server.getPlayerManager().getPlayerList())if(server.getPlayerManager().isOperator(op.getGameProfile()))op.sendMessage(Text.literal("[Moderación] Muerte #"+clip.id+" de "+clip.name+". /tecni replays ver "+clip.id),false);
    }
    private static void retain(ModerationStore.Clip clip,String path){clip.segments.add(path);Moderation.store.data.segments.get(path).retained=true;}
    private static void rotate(ServerPlayerEntity p,Session session) {
        finish(p.getUuid(),session,true);removeUnused(session.previous);
        if(!p.isSpectator())try{begin(p,session.current);}catch(Exception e){retryAt.put(p.getUuid(),System.currentTimeMillis()+30_000);warn(p,e);}
        else removeUnused(session.current);
    }
    @SuppressWarnings("unchecked")
    private static void finish(UUID player,Session session,boolean save) {
        active.remove(player);var segment=Moderation.store.data.segments.get(session.current);segment.finished=System.currentTimeMillis();segment.state="saving";Moderation.store.save();
        try {
            if(stopped(session.recorder))return; // ServerReplay may have already closed on disconnect.
            var future=(CompletableFuture<Long>)call(session.recorder,"stop",new Class<?>[]{boolean.class},save);
            future.whenComplete((size,error)->completions.add(()->{if(Moderation.store==null)return;segment.state=error==null?(save?"ready":"discarded"):"error";if(error!=null)Hardcore.LOG.error("Cannot finalize replay {}",segment.path,error);repair();}));
        }catch(Exception e){Hardcore.LOG.error("Replay closing failed: {}",session.current,e);}
    }
    static void disconnect(ServerPlayerEntity p) {
        var session=active.get(p.getUuid());if(session==null)return;
        finish(p.getUuid(),session,Moderation.store.data.segments.get(session.current).retained);removeUnused(session.previous);
    }
    static void shutdown() {
        for(var entry:new ArrayList<>(active.entrySet())){var s=entry.getValue();finish(entry.getKey(),s,Moderation.store.data.segments.get(s.current).retained);removeUnused(s.previous);}
        ready=false;
    }
    private static Path safeFile(String relative){if(!ModerationStore.validPath(relative))throw new IllegalStateException("Invalid replay path");Path path=root.resolve(relative).normalize();if(!path.startsWith(root))throw new IllegalStateException("Outside replay directory");return path;}
    private static void removeUnused(String relative) {
        if(relative==null||Moderation.store==null)return;var segment=Moderation.store.data.segments.get(relative);
        if(segment==null||segment.retained||!Set.of("ready","discarded").contains(segment.state))return;
        try {Path file=safeFile(relative);if(Files.isSymbolicLink(root)||Files.isSymbolicLink(file.getParent())||Files.isSymbolicLink(file))throw new IllegalStateException("Symlink in buffer");Files.deleteIfExists(file);Moderation.store.data.segments.remove(relative);Moderation.store.save();}catch(Exception e){Hardcore.LOG.warn("Cannot clear own non-evidence replay buffer {}",relative,e);}
    }
    private static void repair() {
        if(Moderation.store==null||root==null)return;boolean changed=false;
        for(var segment:Moderation.store.data.segments.values())if(!segment.state.equals("ready")&&Files.isRegularFile(safeFile(segment.path))){segment.state="ready";changed=true;}
        for(var clip:Moderation.store.data.clips.values())if(clip.status.equals("pending")&&!clip.segments.isEmpty()&&clip.segments.stream().allMatch(p->Files.isRegularFile(safeFile(p)))){clip.status="lista";changed=true;}
        if(changed)Moderation.store.save();
        Set<String> buffers=new HashSet<>();for(var session:active.values()){buffers.add(session.current);buffers.add(session.previous);}
        for(var segment:new ArrayList<>(Moderation.store.data.segments.values()))if(!segment.retained&&!buffers.contains(segment.path))removeUnused(segment.path);
    }
    static String status(){return (ready?"Activo":"NO ACTIVO")+" · "+active.size()+" jugadores · "+Moderation.store.data.clips.size()+" muertes registradas"+(failure.isEmpty()?"":" · "+failure);}
    static String viewCommand(ModerationStore.Clip clip,boolean previous) {
        if(clip.segments.isEmpty())return null;String relative=clip.segments.get(previous?0:clip.segments.size()-1);
        if(!Files.isRegularFile(safeFile(relative)))return null;int slash=relative.indexOf('/');String uuid=relative.substring(0,slash),name=relative.substring(slash+1,relative.length()-5);
        return "replay view players "+uuid+" \""+name+"\"";
    }
}
