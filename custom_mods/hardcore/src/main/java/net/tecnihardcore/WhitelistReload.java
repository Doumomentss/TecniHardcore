package net.tecnihardcore;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import java.nio.file.*;

/** Reloads whitelist edits made with AGREGAR_JUGADOR.bat without restarting Minecraft. */
public final class WhitelistReload {
    private static long stamp;
    private static int ticks;
    private WhitelistReload() {}
    public static void init() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> stamp = modified(server.getRunDirectory().toPath().resolve("whitelist.json")));
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (++ticks % 100 != 0) return;
            long current = modified(server.getRunDirectory().toPath().resolve("whitelist.json"));
            if (current == stamp || current == 0) return;
            stamp = current;
            server.getCommandManager().executeWithPrefix(server.getCommandSource(), "whitelist reload");
            Hardcore.LOG.info("Whitelist updated from disk and reloaded");
        });
    }
    private static long modified(Path file) {
        try { return Files.getLastModifiedTime(file).toMillis(); }
        catch (Exception error) { return 0; }
    }
}
