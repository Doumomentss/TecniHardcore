package net.tecnihardcore;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.WorldSavePath;
import net.minecraft.world.Difficulty;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import static net.minecraft.server.command.CommandManager.*;

/** Clock runs only while the server is online; the last completed second is durable. */
public final class PhaseDay {
    private static final class Data {
        int schema=1;
        boolean started;
        long remaining=PhaseRules.DAY_SECONDS;
        long afterGrace;
        Set<String> books=new LinkedHashSet<>();
    }
    private static Data data;
    private static Path file;
    private static long lastNano;
    private static ServerBossBar bar;
    private PhaseDay() {}
    public static boolean grace() { return data!=null&&data.started&&data.remaining>0; }
    public static int day() { return data==null||!data.started||data.remaining>0?0:PhaseRules.day(data.afterGrace); }
    public static void init() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            file=server.getSavePath(WorldSavePath.ROOT).resolve("tecnihardcore-phase.json");
            try {
                data=Files.exists(file)?SoulStore.JSON.fromJson(Files.readString(file),Data.class):new Data();
                if(data==null||data.schema!=1||data.remaining<0||data.remaining>PhaseRules.DAY_SECONDS||data.afterGrace<0||data.books==null)throw new IllegalStateException("Estado de fase inválido");
                for(String id:data.books)UUID.fromString(id);
                if(!Files.exists(file))save();
            }catch(Exception error){throw new IllegalStateException("No se pudo recuperar el día de gracia; se detiene para no regalar tiempo ni vidas",error);}
            bar=new ServerBossBar(Text.literal("Fin del día de gracia"),BossBar.Color.YELLOW,BossBar.Style.PROGRESS);
            lastNano=System.nanoTime();applyDifficulty(server);updateBar(server);
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {if(data!=null)save();});
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {data=null;file=null;bar=null;lastNano=0;});
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity,source,amount) ->
            !(grace()&&entity instanceof PlayerEntity && source.getAttacker() instanceof PlayerEntity));
        ServerTickEvents.END_SERVER_TICK.register(PhaseDay::tick);
        CommandRegistrationCallback.EVENT.register((dispatcher,access,environment) -> dispatcher.register(literal("tecni")
            .then(literal("fase").requires(source->source.hasPermissionLevel(4))
                .then(literal("1").executes(context -> {
                    if(data==null)return 0;
                    if(data.started){context.getSource().sendError(Text.literal("La fase 1 ya comenzó y no se puede reiniciar."));return 0;}
                    data.started=true;data.remaining=PhaseRules.DAY_SECONDS;data.afterGrace=0;save();
                    applyDifficulty(context.getSource().getServer());updateBar(context.getSource().getServer());
                    context.getSource().getServer().getPlayerManager().broadcast(Text.literal("§6Comenzó el día de gracia: 24 horas online sin pérdida de vidas ni PvP."),false);
                    Hardcore.LOG.warn("ADMIN {} started grace day",context.getSource().getName());return 1;
                }))
                .then(literal("estado").executes(context -> {
                    String status=data==null?"sin datos":!data.started?"todavía no comenzó":grace()?"gracia: "+PhaseRules.clock(data.remaining):"día "+day()+" tras la gracia";
                    context.getSource().sendFeedback(()->Text.literal("Fase: "+status),false);return 1;
                })))));
    }
    private static void tick(MinecraftServer server) {
        if(data==null)return;
        long now=System.nanoTime(),elapsed=Math.max(0,(now-lastNano)/1_000_000_000L);
        if(elapsed>0){lastNano+=elapsed*1_000_000_000L;
            if(data.started){long before=data.remaining;data.remaining=Math.max(0,data.remaining-elapsed);data.afterGrace+=Math.max(0,elapsed-before);save();
                if(before>0&&data.remaining==0){applyDifficulty(server);server.getPlayerManager().broadcast(Text.literal("§cTerminó el día de gracia: las muertes vuelven a descontar vidas y el PvP está activo."),false);}
                if(before==0&&data.afterGrace%PhaseRules.DAY_SECONDS<elapsed){applyDifficulty(server);server.getPlayerManager().broadcast(Text.literal("§cDía "+day()+": aumenta la dificultad del mundo."),false);}
            }
            updateBar(server);
        }
        if(server.getTicks()%20!=0||!data.started)return;
        for(ServerPlayerEntity player:server.getPlayerManager().getPlayerList())if(AuthBootstrap.authenticated(player)&&!AuthBootstrap.serverBot(player)&&data.books.add(player.getUuidAsString())){
            save();ItemStack book=book();if(!player.giveItemStack(book))player.dropItem(book,false);
            player.sendMessage(Text.literal("Recibiste el libro Día de gracia."),false);
        }
    }
    private static void updateBar(MinecraftServer server) {
        if(bar==null)return;
        bar.setVisible(grace());
        if(!grace()){for(ServerPlayerEntity p:new ArrayList<>(bar.getPlayers()))bar.removePlayer(p);return;}
        bar.setName(Text.literal("Fin del día de gracia · "+PhaseRules.clock(data.remaining)));
        bar.setPercent(Math.max(0,Math.min(1,data.remaining/(float)PhaseRules.DAY_SECONDS)));
        for(ServerPlayerEntity p:server.getPlayerManager().getPlayerList())if(AuthBootstrap.authenticated(p)&&!bar.getPlayers().contains(p))bar.addPlayer(p);
        for(ServerPlayerEntity p:new ArrayList<>(bar.getPlayers()))if(!AuthBootstrap.authenticated(p)||server.getPlayerManager().getPlayer(p.getUuid())==null)bar.removePlayer(p);
    }
    private static void applyDifficulty(MinecraftServer server) {
        if(data==null||!data.started)return;
        Difficulty difficulty=grace()?Difficulty.EASY:day()==1?Difficulty.NORMAL:Difficulty.HARD;
        server.setDifficulty(difficulty,true);
    }
    private static ItemStack book() {
        ItemStack book=new ItemStack(Items.WRITTEN_BOOK);NbtCompound n=book.getOrCreateNbt();
        n.putString("title","Día de gracia");n.putString("author","TecniHardcore");
        NbtList pages=new NbtList();
        for(String page:new String[]{"DÍA DE GRACIA\n\nDurante las primeras 24 horas de servidor encendido, las muertes no te quitan vidas y el PvP está desactivado. Aprovecha para conocer el mundo y prepararte.",
                "Al terminar el día de gracia, la dificultad pasa a Normal, vuelven las pérdidas de vidas y se activa el PvP. Desde el día siguiente, la dificultad aumenta cada día.\n\nCuanto más te alejes de 0,0, más peligrosos serán los enemigos.\n\nSuerte."})
            pages.add(NbtString.of(Text.Serializer.toJson(Text.literal(page))));
        n.put("pages",pages);return book;
    }
    private static void save() {
        try {
            Path tmp=file.resolveSibling(file.getFileName()+".tmp");
            Files.writeString(tmp,SoulStore.JSON.toJson(data),StandardCharsets.UTF_8);
            try(var channel=java.nio.channels.FileChannel.open(tmp,StandardOpenOption.WRITE)){channel.force(true);}
            Files.move(tmp,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
        }catch(Exception error){if(Hardcore.server!=null)Hardcore.server.stop(false);throw new IllegalStateException("No se pudo guardar el día de gracia",error);}
    }
}
