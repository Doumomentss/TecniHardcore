package net.tecnihardcore;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.damage.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import java.util.*;
import static net.minecraft.server.command.CommandManager.*;
import com.mojang.brigadier.arguments.IntegerArgumentType;

public final class Moderation {
    public static final Identifier SYNC=Hardcore.id("pvp_guard_v1");
    static ModerationStore store;
    private static final Map<UUID,Long> lastOnline=new HashMap<>();
    private static final Map<UUID,Integer> notice=new HashMap<>();
    private static int ticks;
    public static void init() {
        ServerLifecycleEvents.SERVER_STARTED.register(s->{store=new ModerationStore(s.getSavePath(WorldSavePath.ROOT).resolve("tecnihardcore-moderacion.json"));DeathReplays.start(s);});
        ServerLifecycleEvents.SERVER_STOPPING.register(s->{if(store==null)return;for(var p:s.getPlayerManager().getPlayerList())update(p);DeathReplays.shutdown();store.save();});
        ServerLifecycleEvents.SERVER_STOPPED.register(s->{store=null;lastOnline.clear();notice.clear();});
        ServerPlayConnectionEvents.DISCONNECT.register((h,s)->{if(store==null)return;update(h.player);lastOnline.remove(h.player.getUuid());DeathReplays.disconnect(h.player);store.save();});
        ServerTickEvents.END_SERVER_TICK.register(s->{if(store==null)return;ticks++;for(var p:s.getPlayerManager().getPlayerList()){update(p);if(ticks%20==0)send(p);}if(ticks%20==0)DeathReplays.tick();if(ticks%1200==0)store.save();});
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity,source,amount)->{
            if(!(entity instanceof ServerPlayerEntity victim)||store==null)return true;
            UUID aggressor=owner(source.getAttacker());if(aggressor==null||aggressor.equals(victim.getUuid()))return true;
            if(!ModerationRules.blocksPvp(remaining(aggressor),remaining(victim.getUuid())))return true;
            var p=victim.getServer().getPlayerManager().getPlayer(aggressor);
            if(p!=null&&ticks-notice.getOrDefault(aggressor,-100)>=40){notice.put(aggressor,ticks);p.sendMessage(Text.literal("PvP bloqueado: vos o tu objetivo tienen protección tras una muerte PvP."),true);}return false;
        });
        ServerLivingEntityEvents.AFTER_DEATH.register((entity,source)->{
            if(!(entity instanceof ServerPlayerEntity p)||store==null||!AuthBootstrap.authenticated(p))return;
            UUID killer=owner(source.getAttacker());
            if(killer==null&&!source.isOf(DamageTypes.GENERIC_KILL))killer=owner(p.getPrimeAdversary());
            if(killer!=null&&killer.equals(p.getUuid()))killer=null;
            String killerName="";
            if(killer!=null){var attacker=p.getServer().getPlayerManager().getPlayer(killer);killerName=attacker==null?killer.toString():attacker.getName().getString();}
            if(killer!=null&&!p.isCreative()&&!p.isSpectator()){
                store.data.protection.put(p.getUuidAsString(),ModerationRules.PROTECTION_MS);lastOnline.put(p.getUuid(),System.nanoTime());store.save();
                p.sendMessage(Text.literal("Protección tras muerte PvP: 30 minutos de conexión. No podés dañar ni recibir daño de otros jugadores. Mobs y mundo siguen siendo peligrosos."),false);
                Hardcore.LOG.info("MODERATION PvP protection player={} killer={} remainingMs={}",p.getUuid(),killer,ModerationRules.PROTECTION_MS);
            }
            DeathReplays.death(p,source,killer,killerName);
        });
        CommandRegistrationCallback.EVENT.register((d,r,e)->{
            d.register(literal("tecni").then(literal("proteccion").executes(c->{var p=c.getSource().getPlayerOrThrow();if(!AuthBootstrap.authenticated(p))return 0;c.getSource().sendFeedback(()->Text.literal(description(p)),false);return 1;})
                .then(argument("jugador",net.minecraft.command.argument.EntityArgumentType.player()).requires(s->s.hasPermissionLevel(2)).executes(c->{var p=net.minecraft.command.argument.EntityArgumentType.getPlayer(c,"jugador");c.getSource().sendFeedback(()->Text.literal(description(p)),false);return 1;}))));
            d.register(literal("tecni").then(literal("replays").requires(s->s.hasPermissionLevel(4))
                .then(literal("estado").executes(c->{c.getSource().sendFeedback(()->Text.literal(DeathReplays.status()),false);return 1;}))
                .then(literal("listar").executes(c->{var clips=new ArrayList<>(store.data.clips.values());Collections.reverse(clips);clips.stream().limit(12).forEach(clip->c.getSource().sendFeedback(()->Text.literal("#"+clip.id+" · "+clip.name+" · "+clip.damage+" · "+clip.status+" · /tecni replays ver "+clip.id),false));return clips.size();}))
                .then(literal("ver").then(argument("id",IntegerArgumentType.integer(1)).executes(c->view(c.getSource(),IntegerArgumentType.getInteger(c,"id"),false))
                    .then(literal("antes").executes(c->view(c.getSource(),IntegerArgumentType.getInteger(c,"id"),true)))))));
        });
    }
    private static int view(net.minecraft.server.command.ServerCommandSource source,int id,boolean previous) {
        var clip=store.data.clips.get(Integer.toString(id));if(clip==null){source.sendError(Text.literal("No existe esa muerte."));return 0;}
        String command=DeathReplays.viewCommand(clip,previous);if(command==null){source.sendError(Text.literal("Replay no disponible: "+clip.status+". "+clip.error));return 0;}
        if(source.getEntity()==null){source.sendFeedback(()->Text.literal("En el juego: /"+command),false);return 1;}
        return source.getServer().getCommandManager().executeWithPrefix(source,command);
    }
    static UUID owner(Entity entity){return entity instanceof ServerPlayerEntity p?p.getUuid():entity instanceof TameableEntity pet?pet.getOwnerUuid():null;}
    static long remaining(UUID player){return store==null?0:store.data.protection.getOrDefault(player.toString(),0L);}
    private static void update(ServerPlayerEntity p) {
        long now=System.nanoTime();Long prior=lastOnline.put(p.getUuid(),now);
        if(!AuthBootstrap.authenticated(p)){lastOnline.remove(p.getUuid());return;}
        long before=remaining(p.getUuid());if(before==0)return;
        long after=ModerationRules.remaining(before,prior==null?0:(now-prior)/1_000_000,true);
        if(after==0){store.data.protection.remove(p.getUuidAsString());p.sendMessage(Text.literal("Terminó tu protección tras muerte PvP. PvP habilitado."),false);store.save();}
        else store.data.protection.put(p.getUuidAsString(),after);
    }
    private static String description(ServerPlayerEntity p){long seconds=(remaining(p.getUuid())+999)/1000;return p.getName().getString()+": "+(seconds==0?"sin protección PvP":String.format("protección PvP %d:%02d de conexión · daño de mobs y mundo activo",seconds/60,seconds%60));}
    private static void send(ServerPlayerEntity p){if(ClientPlaySupported(p)){var b=PacketByteBufs.create();b.writeVarInt((int)((remaining(p.getUuid())+999)/1000));ServerPlayNetworking.send(p,SYNC,b);}}
    private static boolean ClientPlaySupported(ServerPlayerEntity p){return AuthBootstrap.authenticated(p)&&ServerPlayNetworking.canSend(p,SYNC);}
}
