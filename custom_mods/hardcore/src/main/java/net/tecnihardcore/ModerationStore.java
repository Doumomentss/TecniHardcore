package net.tecnihardcore;

import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Private moderation ledger; never included in the server status or distributed pack. */
public final class ModerationStore {
    public static final class Clip {
        public String id="", player="", name="", killer="", killerName="", damage="", dimension="", status="pending", error="";
        public long time, markerMillis;
        public double x,y,z;
        public List<String> segments=new ArrayList<>();
    }
    public static final class Segment {
        public String path="", player="", state="recording";
        public long started, finished;
        public boolean retained;
    }
    public static final class Data {
        public int schema=1, nextId=1;
        public Map<String,Long> protection=new LinkedHashMap<>();
        public Map<String,Clip> clips=new LinkedHashMap<>();
        public Map<String,Segment> segments=new LinkedHashMap<>();
    }
    public Data data;
    private final Path file;
    public ModerationStore(Path file) {
        this.file=file;
        try {
            data=Files.exists(file)?SoulStore.JSON.fromJson(Files.readString(file,StandardCharsets.UTF_8),Data.class):new Data();
            if(data==null||data.schema!=1||data.protection==null||data.clips==null||data.segments==null||data.nextId<1)throw new IllegalStateException("Invalid moderation ledger");
            for(var e:data.protection.entrySet()){UUID.fromString(e.getKey());if(e.getValue()==null||e.getValue()<0||e.getValue()>ModerationRules.PROTECTION_MS)throw new IllegalStateException("Invalid protection");}
            for(var e:data.segments.entrySet()){if(!validPath(e.getKey())||!e.getKey().equals(e.getValue().path))throw new IllegalStateException("Invalid replay path");UUID.fromString(e.getValue().player);}
            for(var e:data.clips.entrySet()){if(!e.getKey().matches("[1-9][0-9]*")||!e.getKey().equals(e.getValue().id)||e.getValue().segments.stream().anyMatch(p->!validPath(p)))throw new IllegalStateException("Invalid clip");}
        } catch(Exception e){throw new IllegalStateException("Cannot read moderation ledger; refusing to reset protection or evidence",e);}
    }
    public static boolean validPath(String p) {
        return p!=null&&p.matches("[a-f0-9-]{36}/[A-Za-z0-9_.() -]+\\.mcpr")&&!p.contains("..");
    }
    public synchronized void save() {
        try {
            Files.createDirectories(file.getParent());Path temp=file.resolveSibling(file.getFileName()+".tmp");
            Files.writeString(temp,SoulStore.JSON.toJson(data),StandardCharsets.UTF_8);
            try(var channel=java.nio.channels.FileChannel.open(temp,StandardOpenOption.WRITE)){channel.force(true);}
            if(Files.exists(file))Files.copy(file,file.resolveSibling(file.getFileName()+".previous"),StandardCopyOption.REPLACE_EXISTING);
            Files.move(temp,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
        }catch(Exception e){if(Hardcore.server!=null)Hardcore.server.stop(false);throw new IllegalStateException("Cannot save moderation ledger",e);}
    }
}
