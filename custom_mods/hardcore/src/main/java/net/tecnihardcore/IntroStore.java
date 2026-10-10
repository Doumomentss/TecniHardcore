package net.tecnihardcore;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** One-time completion is scoped to this world, never to the EasyAuth account database. */
final class IntroStore {
    private static final class Data {
        int schema = 1;
        String season = UUID.randomUUID().toString();
        Set<String> completed = new LinkedHashSet<>();
    }
    private final Path file;
    private Data data;

    IntroStore(Path worldRoot) {
        file = worldRoot.resolve("tecnihardcore-intro.json");
        try {
            data = Files.exists(file) ? SoulStore.JSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), Data.class) : new Data();
            if (data == null || data.schema != 1 || data.completed == null) throw new IllegalStateException("Estado de introducción inválido");
            UUID.fromString(data.season);
            for (String id : data.completed) UUID.fromString(id);
            if (!Files.exists(file)) save();
        } catch (Exception e) { throw new IllegalStateException("No se puede leer la introducción; no se reiniciará el progreso silenciosamente", e); }
    }

    String season() { return data.season; }
    boolean completed(UUID player) { return data.completed.contains(player.toString()); }
    void complete(UUID player) { if (data.completed.add(player.toString())) save(); }
    void reset(UUID player) { if (data.completed.remove(player.toString())) save(); }

    private void save() {
        try {
            Files.createDirectories(file.getParent());
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(tmp, SoulStore.JSON.toJson(data), StandardCharsets.UTF_8);
            try (var channel = java.nio.channels.FileChannel.open(tmp, StandardOpenOption.WRITE)) { channel.force(true); }
            Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) { throw new IllegalStateException("No se pudo guardar la introducción", e); }
    }
}
