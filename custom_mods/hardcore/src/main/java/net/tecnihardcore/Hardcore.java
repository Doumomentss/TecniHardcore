package net.tecnihardcore;

import com.google.gson.*;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.entity.*;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.mob.ElderGuardianEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.*;
import net.minecraft.item.*;
import net.minecraft.nbt.*;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.*;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.scoreboard.*;
import net.minecraft.server.*;
import net.minecraft.server.command.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.*;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.world.*;
import org.slf4j.*;
import java.util.*;
import java.nio.file.*;
import static net.minecraft.server.command.CommandManager.*;

public final class Hardcore implements ModInitializer {
    public static final Logger LOG = LoggerFactory.getLogger("TecniHardcore");
    public static final Identifier SYNC = id("soul"), ACTIVATE = id("activate");
    public static final Item BRASA = item("brasa"), BASTION = item("bastion"), ECO = item("eco");
    public static final Item SEAL_BRASA = item("sello_brasa"), SEAL_BASTION = item("sello_bastion"), SEAL_ECO = item("sello_eco");
    public static final Item HEART = Registry.register(Registries.ITEM,id("corazon_sagrado"),new SacredHeartItem());
    public static MinecraftServer server;
    public static SoulStore souls;
    public static volatile String statusJson = "{}";
    private static int ticks;
    public static Identifier id(String path) { return new Identifier("tecnihardcore", path); }
    private static Item item(String path) { return Registry.register(Registries.ITEM, id(path), new Item(new Item.Settings().maxCount(1).fireproof().rarity(Rarity.EPIC))); }

    @Override public void onInitialize() {
        Sanctuaries.init(); RitualNetwork.init(); SanctuaryEffects.init(); SpawnProtection.init(); TotemBoard.init(); TrialBoss.init(); TrialShard.init(); TrialArena.init(); BossRoots.init(); RescueBeacon.init(); LibraryShutdown.init(); Expansion.init(); EventDirector.init();
        for(String sound:new String[]{"boss.wake","boss.strike","boss.melee","boss.warning","boss.prison","boss.volley","boss.phase","boss.transform","boss.death","disaster.wind","disaster.quake","disaster.rain","disaster.thunder","disaster.impact","disaster.alarm","disaster.storm"})Registry.register(Registries.SOUND_EVENT,id(sound),SoundEvent.of(id(sound)));
        ServerLifecycleEvents.SERVER_STARTED.register(s -> {
            server = s;
            AuthBootstrap.start(s);
            souls = new SoulStore(s.getSavePath(WorldSavePath.ROOT).resolve("tecnihardcore-souls.json"));
            Sanctuaries.repairKnown(s);
            Rituals.recoverPayments(s);
            // Old functions are replaced during migration; fail closed if old tick logic survived.
            var old = s.getSavePath(WorldSavePath.DATAPACKS).resolve("hardcore_3_vidas/data/hardcore/functions/tick.mcfunction");
            try { if (Files.exists(old) && Files.readString(old).contains("function hardcore:on_death"))
                throw new IllegalStateException("Legacy death logic is still enabled; run migration before starting");
            } catch (java.io.IOException e) { throw new IllegalStateException(e); }
            s.getGameRules().get(GameRules.DO_IMMEDIATE_RESPAWN).set(true, s);
            publish();
            BackupService.start(s);
            LOG.info("TecniHardcore 2.6.0: unlimited resurrections and Sanctuaries of Souls ready");
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(s -> { if (souls != null) souls.save(); BackupService.stop(); });
        ServerPlayConnectionEvents.JOIN.register((h, sender, s) -> {
            AuthBootstrap.welcome(h.player);
            SoulStore.Soul soul = soul(h.player);
            soul.seen = System.currentTimeMillis(); souls.save();
            Rituals.cleanOrphanMarkers(h.player);
            if (soul.lives == 0 && AuthBootstrap.authenticated(h.player)) h.player.changeGameMode(GameMode.SPECTATOR);
            send(h.player); publish();
        });
        ServerPlayConnectionEvents.DISCONNECT.register((h,s)->Rituals.cancelFor(h.player.getUuid()));
        ServerPlayerEvents.AFTER_RESPAWN.register((old, player, alive) -> {
            player.changeGameMode(soul(player).lives == 0 ? GameMode.SPECTATOR : GameMode.SURVIVAL);
            send(player);
        });
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayerEntity player) {
                SoulStore.Soul soul = soul(player);
                soul.lives = Rules.afterDeath(soul.lives); soul.seen = System.currentTimeMillis();
                Rituals.cancelFor(player.getUuid());
                souls.save(); send(player); publish();
                LOG.info("Death: {} now has {} lives", player.getName().getString(), soul.lives);
                player.sendMessage(Text.literal(soul.lives == 0 ? "Has sido eliminado. Solo un ritual puede devolverte una vida." : "Te quedan " + soul.lives + " vidas."), false);
            }
            if (source.getAttacker() instanceof ServerPlayerEntity killer) {
                String boss = entity instanceof WitherEntity ? "brasa" : entity instanceof EnderDragonEntity ? "bastion" : entity instanceof ElderGuardianEntity ? "eco" : null;
                if (boss != null && soul(killer).bosses.add(boss)) {
                    souls.save();
                    Item seal = boss.equals("brasa") ? SEAL_BRASA : boss.equals("bastion") ? SEAL_BASTION : SEAL_ECO;
                    killer.giveItemStack(new ItemStack(seal));
                    killer.sendMessage(Text.literal("Primer triunfo: has obtenido un sello de " + boss + "."), false);
                }
            }
        });
        ServerTickEvents.END_SERVER_TICK.register(s -> {
            Rituals.tick(s);
            if (++ticks % 20 == 0) {
                // Disconnecting an outdated client removes it from the live player list.
                for (ServerPlayerEntity p : new ArrayList<>(s.getPlayerManager().getPlayerList())) {
                    if(p.age>120&&!RitualNetwork.compatible(p)){p.networkHandler.disconnect(Text.literal("Necesitas TecniHardcore 2.6.0. Cierra el juego y ejecuta el launcher actualizado para instalar el paquete."));continue;}
                    Rituals.finishRecovery(p);
                    if(soul(p).lives==0 && AuthBootstrap.authenticated(p) && !p.isSpectator())p.changeGameMode(GameMode.SPECTATOR);
                    soul(p).seen=System.currentTimeMillis();send(p);
                }
                publish();
                BackupService.tick(s);
            }
        });
        CommandRegistrationCallback.EVENT.register((dispatcher, access, env) -> {
            dispatcher.register(literal("tecni").then(literal("guia").executes(c -> { c.getSource().getPlayerOrThrow().giveItemStack(guide()); return 1; }))
                .then(literal("santuario").requires(s->s.hasPermissionLevel(2))
                    .then(literal("localizar").executes(c->{var pos=Sanctuaries.nearest(c.getSource().getWorld(),BlockPos.ofFloored(c.getSource().getPosition()));c.getSource().sendFeedback(()->Text.literal(pos==null?"No hay santuarios descubiertos en esta dimensión.":"Santuario: "+pos.toShortString()),false);return pos==null?0:1;}))
                    .then(literal("crear").then(argument("pos",net.minecraft.command.argument.BlockPosArgumentType.blockPos()).executes(c->{var w=c.getSource().getWorld();var p=net.minecraft.command.argument.BlockPosArgumentType.getLoadedBlockPos(c,"pos");String problem=Sanctuaries.problem(w,p);if(problem!=null){c.getSource().sendError(Text.literal(problem));return 0;}if(!Sanctuaries.place(w,p))return 0;LOG.warn("ADMIN {} placed sanctuary {}",c.getSource().getName(),p);c.getSource().sendFeedback(()->Text.literal("Santuario creado: "+p.toShortString()),true);return 1;}))))
                .then(literal("ritual").then(argument("objetivo", net.minecraft.command.argument.EntityArgumentType.player()).executes(c ->
                    Rituals.begin(c.getSource().getPlayerOrThrow(), net.minecraft.command.argument.EntityArgumentType.getPlayer(c,"objetivo")))))
                .then(literal("vidas").requires(s -> s.hasPermissionLevel(2)).then(argument("jugador", net.minecraft.command.argument.EntityArgumentType.player())
                    .executes(c -> { var p = net.minecraft.command.argument.EntityArgumentType.getPlayer(c,"jugador"); c.getSource().sendFeedback(() -> Text.literal(p.getName().getString()+": "+soul(p).lives+"/3, resurrecciones: "+soul(p).resurrections), false); return 1; })
                    .then(argument("cantidad", IntegerArgumentType.integer(0,3)).executes(c -> {
                        var p = net.minecraft.command.argument.EntityArgumentType.getPlayer(c,"jugador"); var entry = soul(p);
                        int before = entry.lives; entry.lives = IntegerArgumentType.getInteger(c,"cantidad"); souls.save();
                        p.changeGameMode(entry.lives == 0 ? GameMode.SPECTATOR : GameMode.SURVIVAL);
                        LOG.warn("ADMIN {} changed {} lives {} -> {}", c.getSource().getName(), p.getUuid(), before, entry.lives);
                        send(p); publish(); return 1;
                    }))))
                .then(literal("backup").requires(s -> s.hasPermissionLevel(4)).executes(c -> { BackupService.backup(c.getSource().getServer()); return 1; })));
        });
        ExpansionQa.init();
    }

    public static SoulStore.Soul soul(ServerPlayerEntity p) {
        if (souls == null) throw new IllegalStateException("Soul store not ready");
        return souls.data.players.computeIfAbsent(p.getUuidAsString(), uuid -> {
            SoulStore.Soul entry = new SoulStore.Soul(); entry.name = p.getGameProfile().getName();
            Scoreboard board = p.getServer().getScoreboard(); ScoreboardObjective obj = board.getNullableObjective("hc_lives");
            if (obj != null && board.playerHasObjective(entry.name, obj)) entry.lives = Rules.clampLives(board.getPlayerScore(entry.name,obj).getScore());
            return entry;
        });
    }
    public static void send(ServerPlayerEntity p) {
        var soul = soul(p);
        Scoreboard board = p.getServer().getScoreboard(); var obj = board.getNullableObjective("hc_lives");
        if (obj == null) obj = board.addObjective("hc_lives", ScoreboardCriterion.DUMMY, Text.literal("Vidas"), ScoreboardCriterion.RenderType.INTEGER);
        board.getPlayerScore(p.getGameProfile().getName(),obj).setScore(soul.lives);
        board.setObjectiveSlot(0,obj);
        if (ServerPlayNetworking.canSend(p,RitualNetwork.SOUL)) {
            PacketByteBuf buf = PacketByteBufs.create(); buf.writeInt(soul.lives); buf.writeVarInt(soul.resurrections);
            boolean arena=TrialBoss.arena(p);buf.writeLong(BossCombat.remaining(System.currentTimeMillis(),soul.totemUsedAt,arena));buf.writeBoolean(arena); ServerPlayNetworking.send(p,RitualNetwork.SOUL,buf);
        }
    }
    public static void publish() {
        if (souls == null) return;
        JsonObject root = new JsonObject(); root.addProperty("protocol",2); root.addProperty("packVersion","2.6.0"); root.addProperty("updatedAt", System.currentTimeMillis());
        JsonArray players = new JsonArray();
        souls.data.players.entrySet().stream().sorted(Comparator.comparingLong((Map.Entry<String,SoulStore.Soul> e) -> e.getValue().seen).reversed()).limit(128).forEach(e -> {
            JsonObject v=new JsonObject(); v.addProperty("name",e.getValue().name); v.addProperty("lives",e.getValue().lives); v.addProperty("resurrections",e.getValue().resurrections); v.addProperty("seenAt",e.getValue().seen); var online=server.getPlayerManager().getPlayer(java.util.UUID.fromString(e.getKey()));v.addProperty("online",online!=null&&AuthBootstrap.authenticated(online));players.add(v);
        });
        root.add("players",players); root.add("news",PublicNews.get()); statusJson=root.toString();
    }
    public static boolean isRelic(Item item) { return item==BRASA || item==BASTION || item==ECO; }
    public static boolean blocked(ServerPlayerEntity p) { return BossCombat.remaining(System.currentTimeMillis(),soul(p).totemUsedAt,TrialBoss.arena(p))>0; }
    public static void cooldown(ServerPlayerEntity p) { soul(p).totemUsedAt=System.currentTimeMillis(); soul(p).totemReadyAt=soul(p).totemUsedAt+Rules.COOLDOWN_MS; souls.save(); send(p); }
    public static boolean useRelic(ServerPlayerEntity p, DamageSource source) {
        if (source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY) || blocked(p)) return false;
        for (Hand hand : Hand.values()) {
            ItemStack held=p.getStackInHand(hand); Item item=held.getItem();
            if (!isRelic(item)) continue;
            ItemStack visual=held.copy(); cooldown(p); held.decrement(1);
            p.setHealth(4F); p.clearStatusEffects();
            if (item==BRASA) { effect(p,StatusEffects.FIRE_RESISTANCE,30,0); effect(p,StatusEffects.WEAKNESS,30,1); }
            if (item==BASTION) { effect(p,StatusEffects.RESISTANCE,10,1); effect(p,StatusEffects.SLOWNESS,20,1); }
            if (item==ECO) { effect(p,StatusEffects.INVISIBILITY,15,0); effect(p,StatusEffects.WEAKNESS,20,1); }
            p.getServerWorld().playSound(null,p.getBlockPos(),SoundEvents.ITEM_TOTEM_USE,SoundCategory.PLAYERS,1,1);
            p.getServerWorld().spawnParticles(net.minecraft.particle.ParticleTypes.TOTEM_OF_UNDYING,p.getX(),p.getY()+1,p.getZ(),60,.4,.7,.4,.1);
            PacketByteBuf buf=PacketByteBufs.create(); buf.writeItemStack(visual); ServerPlayNetworking.send(p,ACTIVATE,buf);
            RitualNetwork.relic(p,item==BRASA?0:item==BASTION?1:2);
            LOG.info("Relic {} saved {}", Registries.ITEM.getId(item), p.getGameProfile().getName());
            return true;
        }
        return false;
    }
    private static void effect(ServerPlayerEntity p, StatusEffect e, int seconds,int amp) { p.addStatusEffect(new StatusEffectInstance(e,seconds*20,amp)); }
    public static ItemStack guide() {
        ItemStack book=new ItemStack(Items.WRITTEN_BOOK); NbtCompound n=book.getOrCreateNbt(); n.putString("title","Guía TecniHardcore"); n.putString("author","TecniHardcore"); n.putInt("TecniGuideVersion",25); NbtList pages=new NbtList();
        for(String page : new String[]{"TECNIHARDCORE\nTres vidas. Cada muerte real resta una. A cero, espectador. Un tótem no devuelve vidas.\n\n/tecni guia: este libro.\n/tecni-video: probar el video.",
            "RELIQUIAS\nBrasa: fuego 30s, debilidad II 30s.\nBastión: resistencia II 10s, lentitud II 20s.\nEco: invisibilidad 15s, debilidad II 20s.\nTodas dejan 2 corazones, sin regeneración.",
            "FABRICACIÓN\nUn tótem + un lingote de netherita + una estrella del Nether + sello.\nPrimer Wither: Brasa.\nPrimer dragón: Bastión.\nPrimer guardián anciano: Eco.\nEl sello se entrega al autor del golpe final.",
            "LÍMITES\nTodos los tótems comparten 5 minutos de enfriamiento; dentro de 48 bloques de un jefe en combate, 1 minuto desde la última activación, incluso el vanilla. Se consume estando en una mano. No funciona en el vacío ni con /kill. Los sellos se obtienen una vez por jugador y jefe.",
            "SANTUARIOS DE LAS ALMAS\nPuedes resucitar tantas veces como tus aliados paguen el ritual, solo estando eliminado.\nCorazón Sagrado: 4 estrellas + 4 lingotes de netherita + cristal del End.\nVe al santuario central del spawn, en 0, 0.",
            "RITUAL\nSostén el corazón e interactúa con el núcleo. Elige un eliminado cercano y confirma. También: /tecni ritual Nombre.\nAmbos a menos de 4 bloques, durante 30s. Daño, distancia o desconexión cancelan sin coste. Vuelves con 1 vida.",
            "LA CÚPULA\nEl santuario oscurece 32 bloques a su alrededor. A los 10s comienza la cámara de los participantes.\nEl alma aparece en lo alto y desciende con su skin y aura azul.\nEsc recupera tu cámara sin cancelar.\n/tecni-efectos ajusta las partículas.",
            "BALIZA DE AUXILIO\n8 lingotes de cobre alrededor de un fragmento de eco.\nÚsala para señalar tu posición a compañeros autenticados a 256 bloques, durante 60s.\n16 usos; 10 min entre señales, incluso al reconectarte. No cura ni transporta.",
            "EXPEDICIONES\nSimply Swords: nuevas armas y movimientos con Better Combat.\nImmersive Armors: armaduras especializadas.\nAdventureZ: criaturas y peligros nuevos.\nConsulta recetas en JEI. El HUD avisa cuando tu armadura tiene menos del 15% de durabilidad.",
            "BESTIAS DE CRISTAL\nCustodio: tirada personal 20/15/8/4/1 % para niveles 1–5; 52 % sin montura. Requiere 5 % de daño efectivo. /tecni recompensas entrega tu botín. Invoca con el objeto y monta con clic. Espacio asciende; Z desciende; R impulsa. Controles configurables.",
            "MONTURAS\nSe pueden intercambiar guardadas. Amatista cura 4 puntos fuera de combate. Para guardar: aterriza, desmonta, espera 15 s sin daño y agáchate + clic. Si muere se pierde. Permiten combatir desde el aire; el Custodio responde con ataques a distancia.",
            "CATACLISMOS\nTornado, terremoto, lluvia ácida, tormenta eléctrica y meteoritos: solo por operador. Pueden consumir vidas; no rompen construcciones. Refúgiate bajo techo de la lluvia ácida y esquiva los círculos anunciados. /tecni-sacudida alterna sacudida de cámara.",
            "EVENTOS\nOjo de la tormenta, Circuito de cristal y Defensa del núcleo. /tecni evento listar muestra la inscripción. /tecni evento entrar y /tecni evento salir. ENSAYO conserva vidas e inventario y no da premios; HARDCORE usa tus vidas y equipo reales. Nuevos muebles en Handcrafted y bloques en Chipped."}) pages.add(NbtString.of(Text.Serializer.toJson(Text.literal(page))));
        n.put("pages",pages); return book;
    }
}
