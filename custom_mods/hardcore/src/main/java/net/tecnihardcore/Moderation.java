package net.tecnihardcore;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.damage.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.item.BlockItem;
import net.minecraft.item.BucketItem;
import net.minecraft.item.FlintAndSteelItem;
import net.minecraft.item.FireChargeItem;
import java.util.*;
import static net.minecraft.server.command.CommandManager.*;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.concurrent.CompletableFuture;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.ServerCommandSource;

public final class Moderation {
    public static final Identifier SYNC=Hardcore.id("pvp_guard_v1");
    static ModerationStore store;
    private static final Map<UUID,Long> lastOnline=new HashMap<>();
    private static final Map<UUID,Integer> notice=new HashMap<>();
    private static final Set<UUID> safeRespawn=new HashSet<>();
    private static int ticks;
    public static void init() {
        ServerLifecycleEvents.SERVER_STARTED.register(s->{store=new ModerationStore(s.getSavePath(WorldSavePath.ROOT).resolve("tecnihardcore-moderacion.json"));DeathReplays.start(s);});
        ServerLifecycleEvents.SERVER_STOPPING.register(s->{if(store==null)return;for(var p:s.getPlayerManager().getPlayerList())update(p);DeathReplays.shutdown();store.save();});
        ServerLifecycleEvents.SERVER_STOPPED.register(s->{store=null;lastOnline.clear();notice.clear();safeRespawn.clear();});
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
                safeRespawn.add(p.getUuid());
                p.sendMessage(Text.literal("Protección tras muerte PvP: 30 minutos de conexión. No podés dañar ni recibir daño de otros jugadores. Mobs y mundo siguen siendo peligrosos."),false);
                Hardcore.LOG.info("MODERATION PvP protection player={} killer={} remainingMs={}",p.getUuid(),killer,ModerationRules.PROTECTION_MS);
            }
            DeathReplays.death(p,source,killer,killerName);
        });
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer,newPlayer,alive)->{
            if(!safeRespawn.remove(newPlayer.getUuid()))return;
            var world=newPlayer.getServer().getOverworld();var pos=world.getSpawnPos();
            newPlayer.teleport(world,pos.getX()+.5,pos.getY(),pos.getZ()+.5,world.getSpawnAngle(),0);
            newPlayer.sendMessage(Text.literal("§6§l[TECNI PROTECCIÓN] §r§eReapareciste en la plaza para evitar abusos dentro de una base enemiga."),false);
        });
        PlayerBlockBreakEvents.BEFORE.register((world,player,pos,state,blockEntity)->
            !(player instanceof ServerPlayerEntity sp)||!consumeProtectionForWorldAction(sp));
        UseBlockCallback.EVENT.register((player,world,hand,hit)->{
            if(!(player instanceof ServerPlayerEntity sp)||remaining(sp.getUuid())==0)return ActionResult.PASS;
            var held=sp.getStackInHand(hand).getItem();
            boolean modifies=held instanceof BlockItem||held instanceof BucketItem||held instanceof FlintAndSteelItem||held instanceof FireChargeItem||world.getBlockEntity(hit.getBlockPos())!=null;
            return modifies&&consumeProtectionForWorldAction(sp)?ActionResult.FAIL:ActionResult.PASS;
        });
        UseItemCallback.EVENT.register((player,world,hand)->{
            if(!(player instanceof ServerPlayerEntity sp))return TypedActionResult.pass(player.getStackInHand(hand));
            var held=sp.getStackInHand(hand);var item=held.getItem();
            boolean modifies=item instanceof BlockItem||item instanceof BucketItem||item instanceof FlintAndSteelItem||item instanceof FireChargeItem;
            return modifies&&consumeProtectionForWorldAction(sp)?TypedActionResult.fail(held):TypedActionResult.pass(held);
        });
        CommandRegistrationCallback.EVENT.register((d,r,e)->{
            d.register(literal("tecni").then(literal("proteccion").executes(c->{var p=c.getSource().getPlayerOrThrow();if(!AuthBootstrap.authenticated(p))return 0;c.getSource().sendFeedback(()->Text.literal(description(p)),false);return 1;})
                .then(argument("jugador",net.minecraft.command.argument.EntityArgumentType.player()).requires(s->s.hasPermissionLevel(2)).executes(c->{var p=net.minecraft.command.argument.EntityArgumentType.getPlayer(c,"jugador");c.getSource().sendFeedback(()->Text.literal(description(p)),false);return 1;}))));
            d.register(literal("tecni").then(literal("replay").requires(s->s.hasPermissionLevel(4))
                .executes(c->{c.getSource().sendFeedback(()->Text.literal("/tecni replay lista Nombre · /tecni replay play Nombre ID · para salir: /replay view close"),false);return 1;})
                .then(literal("estado").executes(c->{c.getSource().sendFeedback(()->Text.literal(DeathReplays.status()),false);return 1;}))
                .then(literal("lista").executes(c->listRecent(c.getSource()))
                    .then(argument("jugador",StringArgumentType.word()).suggests(Moderation::suggestPlayers).executes(c->listPlayer(c.getSource(),StringArgumentType.getString(c,"jugador")))))
                .then(literal("play").then(argument("jugador",StringArgumentType.word()).suggests(Moderation::suggestPlayers)
                    .then(argument("id",IntegerArgumentType.integer(0)).executes(c->play(c.getSource(),StringArgumentType.getString(c,"jugador"),IntegerArgumentType.getInteger(c,"id"))))))));
        });
    }
    private static CompletableFuture<Suggestions> suggestPlayers(CommandContext<ServerCommandSource> context,SuggestionsBuilder builder) {
        Set<String> names=new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        if(store!=null)for(var clip:store.data.clips.values())if(clip.name!=null&&!clip.name.isBlank())names.add(clip.name);
        for(var player:context.getSource().getServer().getPlayerManager().getPlayerList())names.add(player.getName().getString());
        names.addAll(Arrays.asList(context.getSource().getServer().getPlayerManager().getWhitelist().getNames()));
        return CommandSource.suggestMatching(names,builder);
    }
    private static int listRecent(net.minecraft.server.command.ServerCommandSource source) {
        var clips=new ArrayList<>(store.data.clips.values());Collections.reverse(clips);
        if(clips.isEmpty())source.sendFeedback(()->Text.literal("Todavía no hay muertes registradas. "+DeathReplays.status()),false);
        clips.stream().limit(12).forEach(clip->source.sendFeedback(()->Text.literal(clip.name+" · "+clip.damage+" · "+clip.status+" · /tecni replay lista "+clip.name),false));
        return clips.size();
    }
    private static int listPlayer(net.minecraft.server.command.ServerCommandSource source,String name) {
        if(ReplayIndex.ambiguous(store.data.clips.values(),name)){source.sendError(Text.literal("Ese nombre figura con varios UUID; contactá al administrador del registro."));return 0;}
        var history=ReplayIndex.history(store.data.clips.values(),name);
        if(history.isEmpty()){source.sendError(Text.literal("No hay muertes registradas para "+name+"."));return 0;}
        source.sendFeedback(()->Text.literal("Muertes de "+history.get(history.size()-1).name+" ("+history.size()+") · IDs 0–"+(history.size()-1)),false);
        for(int i=0;i<history.size();i++){
            var clip=history.get(i);int id=i;
            source.sendFeedback(()->Text.literal("["+id+"] "+new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(clip.time))+" · "+clip.damage+" · "+clip.status+" · /tecni replay play "+name+" "+id),false);
        }
        return history.size();
    }
    private static int play(net.minecraft.server.command.ServerCommandSource source,String name,int id) {
        if(ReplayIndex.ambiguous(store.data.clips.values(),name)){source.sendError(Text.literal("Ese nombre figura con varios UUID; no se elegirá una replay ambigua."));return 0;}
        var history=ReplayIndex.history(store.data.clips.values(),name);
        if(id>=history.size()){source.sendError(Text.literal("No existe la muerte "+id+" de "+name+". Usá /tecni replay lista "+name));return 0;}
        var clip=history.get(id);
        int result=view(source,Integer.parseInt(clip.id),false);
        if(source.getEntity() instanceof ServerPlayerEntity player)
            DeathReplays.seekBeforeDeath(player,clip,30_000);
        return result;
    }
    private static int view(net.minecraft.server.command.ServerCommandSource source,int id,boolean previous) {
        var clip=store.data.clips.get(Integer.toString(id));if(clip==null){source.sendError(Text.literal("No existe esa muerte."));return 0;}
        String command=DeathReplays.viewCommand(clip,previous);if(command==null){source.sendError(Text.literal("Replay no disponible: "+clip.status+". "+clip.error));return 0;}
        if(source.getEntity()==null){source.sendFeedback(()->Text.literal("En el juego: /"+command),false);return 1;}
        // ServerReplay creates a separate viewing world. Put the moderator in
        // spectator before entering it so the recorded scene is observed and
        // cannot be interacted with like the live survival world.
        if(source.getEntity() instanceof ServerPlayerEntity player) player.changeGameMode(net.minecraft.world.GameMode.SPECTATOR);
        return source.getServer().getCommandManager().executeWithPrefix(source,command);
    }
    static UUID owner(Entity entity){return entity instanceof ServerPlayerEntity p?p.getUuid():entity instanceof TameableEntity pet?pet.getOwnerUuid():null;}
    static long remaining(UUID player){return store==null?0:store.data.protection.getOrDefault(player.toString(),0L);}
    private static boolean consumeProtectionForWorldAction(ServerPlayerEntity player){
        if(store==null||remaining(player.getUuid())==0||player.isCreative()||player.isSpectator())return false;
        store.data.protection.remove(player.getUuidAsString());lastOnline.remove(player.getUuid());store.save();
        player.sendMessage(Text.literal("§6§l[TECNI PROTECCIÓN] §r§cAcción bloqueada: al modificar bloques o contenedores termina tu protección PvP. Repite la acción solo si aceptás quedar expuesto."),false);
        Hardcore.LOG.info("MODERATION PvP protection cancelled by world action player={}",player.getUuid());
        return true;
    }
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
