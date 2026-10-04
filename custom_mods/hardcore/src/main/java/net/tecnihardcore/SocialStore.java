package net.tecnihardcore;

import com.google.gson.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.WorldSavePath;
import net.minecraft.nbt.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** One atomic ledger for teams, wallets, escrow and quests; inventory changes are journaled. */
public final class SocialStore {
    public static final class Team {public String id="",name="",owner="";public Set<String> members=new LinkedHashSet<>();}
    public static final class Truce {public String expelled="",team="",name="";public Set<String> members=new HashSet<>();public int remaining=7200;public boolean notified;}
    public static final class Offer {public String id="",seller="",sellerName="",item="",state="open";public long price,created;}
    public static final class Parcel {public String id="",owner="",item="",reason="";}
    public static final class Business {public int schema=1;public Map<String,Team> teams=new LinkedHashMap<>();public List<Truce> truces=new ArrayList<>();public Map<String,Long> wallets=new HashMap<>();public Map<String,Offer> offers=new LinkedHashMap<>();public Map<String,Parcel> parcels=new LinkedHashMap<>();public Map<String,Set<String>> quests=new HashMap<>();}
    public static final class Pending {public String player="",inventory="",business="",reason="";}
    private static final class FileData {Business data=new Business();Pending pending;}
    public Business data;private Pending pending;private final MinecraftServer server;private final Path file;
    static final Gson JSON=new GsonBuilder().setPrettyPrinting().create();
    public SocialStore(MinecraftServer s){server=s;file=s.getSavePath(WorldSavePath.ROOT).resolve("tecnihardcore-social.json");try{FileData state=Files.exists(file)?JSON.fromJson(Files.readString(file,StandardCharsets.UTF_8),FileData.class):new FileData();if(state==null||state.data==null)throw new IllegalStateException("Missing social ledger");data=state.data;pending=state.pending;validate(data);if(pending!=null)recover();}catch(Exception e){throw new IllegalStateException("Cannot recover social ledger; refusing to reset money/items/teams",e);}}
    public static void validate(Business b){if(b.schema!=1||b.teams==null||b.truces==null||b.wallets==null||b.offers==null||b.parcels==null||b.quests==null)throw new IllegalStateException("Invalid ledger");validateKeys(b);Set<String> seen=new HashSet<>();for(var t:b.teams.values()){UUID.fromString(t.id);UUID.fromString(t.owner);if(!SocialRules.teamName(t.name)||!t.members.contains(t.owner)||t.members.size()>12)throw new IllegalStateException("Invalid team");for(String m:t.members){UUID.fromString(m);if(!seen.add(m))throw new IllegalStateException("Duplicate team membership");}}for(var t:b.truces){UUID.fromString(t.expelled);UUID.fromString(t.team);if(t.remaining<0||t.remaining>7200)throw new IllegalStateException("Invalid truce");for(String m:t.members)UUID.fromString(m);}for(var e:b.wallets.entrySet()){UUID.fromString(e.getKey());SocialRules.transfer(e.getValue(),0);}for(var o:b.offers.values()){UUID.fromString(o.id);UUID.fromString(o.seller);if(o.item.length()>100000||o.price<1||o.price>1_000_000||!Set.of("open","sold","cancelled").contains(o.state))throw new IllegalStateException("Invalid offer");}for(var p:b.parcels.values()){UUID.fromString(p.id);UUID.fromString(p.owner);if(p.item.length()>100000)throw new IllegalStateException("Invalid parcel");}}
    public Business copy(){return JSON.fromJson(JSON.toJson(data),Business.class);}
    static void validateKeys(Business b){
        for(var e:b.teams.entrySet())if(!e.getKey().equals(e.getValue().id))throw new IllegalStateException("Team identity mismatch");
        for(var e:b.offers.entrySet())if(!e.getKey().equals(e.getValue().id))throw new IllegalStateException("Offer identity mismatch");
        for(var e:b.parcels.entrySet())if(!e.getKey().equals(e.getValue().id))throw new IllegalStateException("Parcel identity mismatch");
        for(var e:b.quests.entrySet()){UUID.fromString(e.getKey());if(e.getValue()==null||e.getValue().size()>9)throw new IllegalStateException("Invalid quests");for(String f:e.getValue())if(!f.matches("(sendero|madera|hierro):(active|ready|done)"))throw new IllegalStateException("Unknown quest flag");}
    }
    public void commit(Business next,String reason){if(pending!=null)throw new IllegalStateException("Pending inventory transaction");validate(next);data=next;save();Hardcore.LOG.info("SOCIAL transaction: {}",reason);}
    public void inventory(ServerPlayerEntity player,NbtList after,Business next,String reason){if(pending!=null)throw new IllegalStateException("Pending inventory transaction");validate(next);pending=new Pending();pending.player=player.getUuidAsString();pending.inventory=after.toString();pending.business=JSON.toJson(next);pending.reason=reason;save();try{player.getInventory().readNbt(after);server.getPlayerManager().saveAllPlayerData();data=next;pending=null;save();Hardcore.LOG.info("SOCIAL inventory transaction {}: {}",player.getUuidAsString(),reason);}catch(Exception error){server.stop(false);throw new IllegalStateException("Inventory transaction failed; recovery required",error);}}
    private void recover()throws Exception{UUID.fromString(pending.player);Business next=JSON.fromJson(pending.business,Business.class);validate(next);NbtList inventory=StringNbtReader.parse("{Inventory:"+pending.inventory+"}").getList("Inventory",10);Path player=server.getSavePath(WorldSavePath.ROOT).resolve("playerdata").resolve(pending.player+".dat");if(!Files.exists(player))throw new IllegalStateException("Transaction player data missing");NbtCompound nbt=NbtIo.readCompressed(player.toFile());nbt.put("Inventory",inventory);Path tmp=player.resolveSibling(player.getFileName()+".social-tmp");NbtIo.writeCompressed(nbt,tmp.toFile());try(var c=java.nio.channels.FileChannel.open(tmp,StandardOpenOption.WRITE)){c.force(true);}Files.move(tmp,player,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);data=next;Hardcore.LOG.warn("Recovered interrupted social transaction: {}",pending.reason);pending=null;save();}
    public synchronized void save(){try{validate(data);FileData state=new FileData();state.data=data;state.pending=pending;Path tmp=file.resolveSibling(file.getFileName()+".tmp");Files.writeString(tmp,JSON.toJson(state),StandardCharsets.UTF_8);try(var c=java.nio.channels.FileChannel.open(tmp,StandardOpenOption.WRITE)){c.force(true);}if(Files.exists(file))Files.copy(file,file.resolveSibling(file.getFileName()+".previous"),StandardCopyOption.REPLACE_EXISTING);Files.move(tmp,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(Exception e){Hardcore.LOG.error("Social ledger persistence failed",e);server.stop(false);throw new IllegalStateException("Cannot persist social ledger",e);}}
}
