package net.tecnihardcore;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.WorldSavePath;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

public final class BackupService {
    private static long next;
    public static void start(MinecraftServer s) {
        next=System.currentTimeMillis()+6*60*60*1000L;
        Path folder=s.getRunDirectory().toPath().resolve("backups/tecnihardcore");
        if(Files.isDirectory(folder))try(var files=Files.list(folder)) {
            var last=files.map(p->p.getFileName().toString()).filter(n->n.matches("world-[0-9]+\\.zip")).sorted(Comparator.reverseOrder()).findFirst();
            if(last.isPresent())next=Long.parseLong(last.get().substring(6,last.get().length()-4))+6*60*60*1000L;
        }catch(Exception e){throw new IllegalStateException("Cannot read backup schedule",e);}
    }
    public static void stop() { next=Long.MAX_VALUE; }
    public static void tick(MinecraftServer s) { if(System.currentTimeMillis()>=next) { next=System.currentTimeMillis()+6*60*60*1000L; backup(s); } }
    public static void backup(MinecraftServer s) {
        // Runs on the server thread: flush, then freeze ticks for a consistent snapshot.
        Path world=s.getSavePath(WorldSavePath.ROOT).toAbsolutePath().normalize();
        Path folder=s.getRunDirectory().toPath().toAbsolutePath().resolve("backups/tecnihardcore");
        long start=System.currentTimeMillis();
        try {
            Hardcore.souls.save(); s.getPlayerManager().saveAllPlayerData(); s.save(false,true,true); Files.createDirectories(folder);
            Path archive=folder.resolve("world-"+start+".zip"), tmp=folder.resolve("world-"+start+".partial");
            try(ZipOutputStream out=new ZipOutputStream(Files.newOutputStream(tmp));var paths=Files.walk(world)) {
                for(Path file:paths.filter(Files::isRegularFile).toList()) {
                    if(file.getFileName().toString().equals("session.lock"))continue;
                    out.putNextEntry(new ZipEntry(world.relativize(file).toString().replace('\\','/'))); Files.copy(file,out); out.closeEntry();
                }
            }
            try(ZipFile check=new ZipFile(tmp.toFile())) {
                if(check.getEntry("level.dat")==null)throw new java.io.IOException("Backup lacks level.dat");
                for(var entry:check.stream().toList()) {
                    CRC32 crc=new CRC32();try(var input=check.getInputStream(entry)){byte[] bytes=new byte[8192];int n;while((n=input.read(bytes))!=-1)crc.update(bytes,0,n);}
                    if(crc.getValue()!=entry.getCrc())throw new java.io.IOException("Backup CRC mismatch: "+entry.getName());
                }
            }
            Files.move(tmp,archive,StandardCopyOption.ATOMIC_MOVE);
            next=System.currentTimeMillis()+6*60*60*1000L;
            try(var list=Files.list(folder)) { for(Path old:list.filter(p->p.getFileName().toString().matches("world-[0-9]+\\.zip")).sorted(Comparator.reverseOrder()).skip(7).toList()) Files.delete(old); }
            Hardcore.LOG.info("Consistent backup {} completed in {} ms",archive,System.currentTimeMillis()-start);
        } catch(Exception e) { next=System.currentTimeMillis()+5*60*1000L;Hardcore.LOG.error("Backup failed; previous snapshots retained",e); }
    }
}
