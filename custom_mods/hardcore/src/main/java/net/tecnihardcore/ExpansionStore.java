package net.tecnihardcore;

import com.google.gson.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.WorldSavePath;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Independent of souls. Fail closed instead of resetting rewards or mount ownership. */
public final class ExpansionStore {
    public static final class Mount {
        public int tier;public float health;public String state="guardada",holder="",entity="",dimension="";
        public long boostAt,damageAt,healAt;public double x,y,z;public boolean temporary;
        public boolean returnPending;public double returnX,returnY,returnZ;
    }
    public static final class Reward {
        public String player="",mount="",reason="";public int diamonds,gold,iron;public String trophy="";public String state="pending";
    }
    public static final class Data {
        public int schema=1;public Map<String,Mount> mounts=new LinkedHashMap<>();
        public Map<String,Reward> rewards=new LinkedHashMap<>();public Set<String> defeated=new HashSet<>();
    }
    public final Path file;public final Data data;private final MinecraftServer server;
    private static final Gson JSON=new GsonBuilder().setPrettyPrinting().create();
    public ExpansionStore(MinecraftServer s){server=s;file=s.getSavePath(WorldSavePath.ROOT).resolve("tecnihardcore-expansion.json");try{
        data=Files.exists(file)?JSON.fromJson(Files.readString(file,StandardCharsets.UTF_8),Data.class):new Data();
        if(data==null||data.schema!=1||data.mounts==null||data.rewards==null||data.defeated==null)throw new IllegalStateException("Invalid expansion state");
        validate(data);
    }catch(Exception e){throw new IllegalStateException("Cannot load expansion: refusing to reset mounts/rewards",e);}}
    static void validate(Data state){
        for(var entry:state.mounts.entrySet()){
            UUID.fromString(entry.getKey());var m=entry.getValue();
            if(m==null||m.tier<1||m.tier>5||!Float.isFinite(m.health)||m.health<0||m.health>ExpansionRules.HEALTH[m.tier-1]||!Set.of("guardada","activa","muerta").contains(m.state)||m.holder==null||m.entity==null||m.dimension==null||!Double.isFinite(m.x)||!Double.isFinite(m.y)||!Double.isFinite(m.z))throw new IllegalStateException("Invalid mount "+entry.getKey());
            if(!m.holder.isEmpty())UUID.fromString(m.holder);if(!m.entity.isEmpty())UUID.fromString(m.entity);
            if(m.state.equals("activa")&&(m.entity.isEmpty()||m.holder.isEmpty()||m.dimension.isEmpty()))throw new IllegalStateException("Active mount missing identity");
            if(m.returnPending&&(!Double.isFinite(m.returnX)||!Double.isFinite(m.returnY)||!Double.isFinite(m.returnZ)||m.holder.isEmpty()||m.dimension.isEmpty()))throw new IllegalStateException("Invalid mount return position");
        }
        for(var entry:state.rewards.entrySet()){
            var r=entry.getValue();if(r==null||r.reason==null||r.trophy==null||r.mount==null||r.diamonds<0||r.diamonds>64||r.gold<0||r.gold>64||r.iron<0||r.iron>64||!Set.of("pending","delivering","claimed").contains(r.state))throw new IllegalStateException("Invalid reward "+entry.getKey());
            UUID.fromString(r.player);if(!r.mount.isEmpty()&&!state.mounts.containsKey(r.mount))throw new IllegalStateException("Reward references missing mount");
        }
    }
    public void save(){try{
        validate(data);
        Path tmp=file.resolveSibling(file.getFileName()+".tmp");Files.writeString(tmp,JSON.toJson(data),StandardCharsets.UTF_8);
        try(var c=java.nio.channels.FileChannel.open(tmp,StandardOpenOption.WRITE)){c.force(true);}
        if(Files.exists(file))Files.copy(file,file.resolveSibling(file.getFileName()+".previous"),StandardCopyOption.REPLACE_EXISTING);
        Files.move(tmp,file,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);
    }catch(Exception e){server.stop(false);throw new IllegalStateException("Cannot commit expansion state",e);}}
}
