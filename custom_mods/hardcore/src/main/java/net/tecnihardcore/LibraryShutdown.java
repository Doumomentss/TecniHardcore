package net.tecnihardcore;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
/** Fzzy 0.7.7 stops its watcher but leaves its non-daemon config worker pool alive. */
public final class LibraryShutdown {
    public static void init(){ServerLifecycleEvents.SERVER_STOPPED.register(server->{
        if(!server.isDedicated())return;
        try{
            var type=Class.forName("me.fzzyhmstrs.fzzy_config.util.ThreadingUtils");var instance=type.getField("INSTANCE").get(null);
            var pool=(java.util.concurrent.ExecutorService)type.getMethod("getEXECUTOR$fzzy_config").invoke(instance);
            pool.shutdown();if(!pool.awaitTermination(3,java.util.concurrent.TimeUnit.SECONDS))pool.shutdownNow();
        }catch(ClassNotFoundException ignored){}catch(Exception e){Hardcore.LOG.warn("Config worker shutdown failed: {}",e.getClass().getSimpleName());}
    });}
}
