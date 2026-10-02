package net.tecnihardcore;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Server-only state. Every mutation is durably committed before notifying clients. */
public final class SoulStore {
    public static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
    public static class Soul {
        public String name = "";
        public int lives = 3;
        public boolean revived;
        public int resurrections;
        public long totemReadyAt;
        public long seen;
        public String reviveDimension;
        public int[] revivePosition;
        public Set<String> bosses = new HashSet<>();
    }
    public static class Data {
        public int schema = 2;
        public Map<String, Soul> players = new LinkedHashMap<>();
        public Map<String, String> ritualPayments = new LinkedHashMap<>();
        public Set<String> sanctuaries = new HashSet<>();
    }
    private final Path file;
    public Data data;
    public SoulStore(Path file) {
        this.file = file;
        try {
            data = Files.exists(file) ? JSON.fromJson(Files.readString(file), Data.class) : new Data();
            if (data == null || (data.schema != 1 && data.schema != 2) || data.players == null) throw new IllegalStateException("Invalid soul state");
            if(data.ritualPayments==null)data.ritualPayments=new LinkedHashMap<>();
            if(data.sanctuaries==null)data.sanctuaries=new HashSet<>();
            for (Soul soul : data.players.values()) {
                soul.lives = Rules.clampLives(soul.lives);
                if(data.schema==1 && soul.revived)soul.resurrections=Math.max(1,soul.resurrections);
                soul.resurrections=Math.max(0,soul.resurrections);
            }
            data.schema=2;
        } catch (Exception e) { throw new IllegalStateException("Cannot load " + file + "; refusing to reset lives", e); }
    }
    public void save() {
        try {
            Files.createDirectories(file.getParent());
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(tmp, JSON.toJson(data), StandardCharsets.UTF_8);
            try (var channel = java.nio.channels.FileChannel.open(tmp, StandardOpenOption.WRITE)) { channel.force(true); }
            if (Files.exists(file)) Files.copy(file, file.resolveSibling(file.getFileName()+".previous"), StandardCopyOption.REPLACE_EXISTING);
            Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            if(Hardcore.server!=null)Hardcore.server.stop(false);
            throw new IllegalStateException("Cannot persist souls; stopping server to protect state", e);
        }
    }
}
