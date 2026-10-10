package net.tecnihardcore;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.networking.v1.*;
import net.fabricmc.fabric.api.event.player.*;
import net.minecraft.util.ActionResult;
import net.minecraft.util.TypedActionResult;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import java.util.*;
import static net.minecraft.server.command.CommandManager.*;

/** Server-authoritative, independent welcome sequence for the new season. */
public final class IntroServer {
    static final Identifier READY=Hardcore.id("intro_ready_v1"), PREPARE=Hardcore.id("intro_prepare_v1"), START=Hardcore.id("intro_start_v1"), ACK=Hardcore.id("intro_ack_v1"), DONE=Hardcore.id("intro_done_v1"), RESET=Hardcore.id("intro_reset_v1");
    private static final BlockPos ARRIVAL = new BlockPos(0,96,20);
    private static final Map<UUID,Session> sessions = new HashMap<>();
    private static final Set<UUID> prepared = new HashSet<>();
    private static IntroStore store;
    private record Session(UUID id, UUID player, long started, Vec3d anchor, boolean preview) {}
    private IntroServer() {}

    static void init() {
        ServerLifecycleEvents.SERVER_STARTED.register(s -> store = new IntroStore(s.getSavePath(WorldSavePath.ROOT)));
        ServerLifecycleEvents.SERVER_STOPPED.register(s -> { sessions.clear();prepared.clear();store=null; });
        ServerPlayConnectionEvents.DISCONNECT.register((h,s) -> {sessions.remove(h.player.getUuid());prepared.remove(h.player.getUuid());});
        ServerPlayNetworking.registerGlobalReceiver(READY,(s,p,h,b,r) -> s.execute(() -> ready(p)));
        ServerPlayNetworking.registerGlobalReceiver(ACK,(s,p,h,b,r) -> {
            UUID id=b.readUuid();s.execute(() -> acknowledge(p,id));
        });
        ServerTickEvents.END_SERVER_TICK.register(IntroServer::tick);
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity,source,amount) -> !(entity instanceof ServerPlayerEntity p && sessions.containsKey(p.getUuid())));
        PlayerBlockBreakEvents.BEFORE.register((w,p,pos,state,be)->!sessions.containsKey(p.getUuid()));
        UseBlockCallback.EVENT.register((p,w,hand,hit)->sessions.containsKey(p.getUuid())?ActionResult.FAIL:ActionResult.PASS);
        UseEntityCallback.EVENT.register((p,w,hand,target,hit)->sessions.containsKey(p.getUuid())?ActionResult.FAIL:ActionResult.PASS);
        AttackEntityCallback.EVENT.register((p,w,hand,target,hit)->sessions.containsKey(p.getUuid())?ActionResult.FAIL:ActionResult.PASS);
        UseItemCallback.EVENT.register((p,w,hand)->sessions.containsKey(p.getUuid())?TypedActionResult.fail(p.getStackInHand(hand)):TypedActionResult.pass(p.getStackInHand(hand)));
        CommandRegistrationCallback.EVENT.register((dispatcher,registry,environment) -> dispatcher.register(literal("tecni")
            .then(literal("intro").requires(source -> source.hasPermissionLevel(4))
                .then(literal("probar").executes(c -> start(c.getSource().getPlayerOrThrow(),true)))
                .then(literal("reiniciar").then(argument("jugador",EntityArgumentType.player()).executes(c -> {
                    ServerPlayerEntity target=EntityArgumentType.getPlayer(c,"jugador");
                    if(store==null)return 0;
                    sessions.remove(target.getUuid());prepared.remove(target.getUuid());store.reset(target.getUuid());send(target,RESET,null);
                    c.getSource().sendFeedback(() -> Text.literal("Introducción reiniciada para "+target.getName().getString()+"."),true);
                    return 1;
                }))))));
    }

    private static void ready(ServerPlayerEntity p) {
        if(store==null || AuthBootstrap.serverBot(p) || !AuthBootstrap.authenticated(p) || p.getWorld().getRegistryKey()!=World.OVERWORLD || p.age<40)return;
        if(!ServerPlayNetworking.canSend(p,START))return;
        if(store.completed(p.getUuid())) { send(p,DONE,null); return; }
        if(!prepared.contains(p.getUuid())){
            var w=p.getServerWorld();
            p.teleport(w,ARRIVAL.getX()+.5,ARRIVAL.getY(),ARRIVAL.getZ()+.5,180,0);
            prepared.add(p.getUuid());
            send(p,PREPARE,null);
            return;
        }
        if(!sessions.containsKey(p.getUuid()))start(p,false);
    }

    private static int start(ServerPlayerEntity p,boolean preview) {
        if(store==null || !AuthBootstrap.authenticated(p) || p.getWorld().getRegistryKey()!=World.OVERWORLD || sessions.containsKey(p.getUuid()) || !ServerPlayNetworking.canSend(p,START))return 0;
        var w=p.getServerWorld();
        if(!w.isChunkLoaded(ARRIVAL) || !w.getBlockState(ARRIVAL.down()).isSolidBlock(w,ARRIVAL.down()) || !w.isAir(ARRIVAL) || !w.isAir(ARRIVAL.up())) {
            p.sendMessage(Text.literal("La plaza no está preparada para la introducción. Contacta a un operador."),false);
            Hardcore.LOG.error("Intro spawn is not safe at {}",ARRIVAL);return 0;
        }
        Vec3d anchor=Vec3d.ofBottomCenter(ARRIVAL);
        p.teleport(w,anchor.x,anchor.y,anchor.z,180,0);
        p.setVelocity(Vec3d.ZERO);p.fallDistance=0;
        Session session=new Session(UUID.randomUUID(),p.getUuid(),System.currentTimeMillis(),anchor,preview);
        sessions.put(p.getUuid(),session);
        PacketByteBuf b=PacketByteBufs.create();b.writeUuid(session.id);b.writeString(store.season());b.writeBlockPos(ARRIVAL);b.writeVarLong(IntroRules.DURATION_MS);
        ServerPlayNetworking.send(p,START,b);
        return 1;
    }

    private static void acknowledge(ServerPlayerEntity p,UUID id) {
        Session session=sessions.get(p.getUuid());
        if(session==null || !session.id.equals(id) || !IntroRules.mayCommit(System.currentTimeMillis()-session.started) || !AuthBootstrap.authenticated(p))return;
        if(!session.preview)store.complete(p.getUuid());
        sessions.remove(p.getUuid());prepared.remove(p.getUuid());send(p,DONE,id);
        Hardcore.LOG.info("Intro completed for {} (preview={})",p.getUuid(),session.preview);
    }

    private static void tick(MinecraftServer server) {
        long now=System.currentTimeMillis();
        for(Session session:new ArrayList<>(sessions.values())) {
            ServerPlayerEntity p=server.getPlayerManager().getPlayer(session.player);
            if(p==null || !AuthBootstrap.authenticated(p) || p.getWorld().getRegistryKey()!=World.OVERWORLD) {sessions.remove(session.player);continue;}
            if(now-session.started>IntroRules.TIMEOUT_MS) {
                sessions.remove(session.player);
                p.networkHandler.disconnect(Text.literal("La introducción no pudo completarse. Abre el launcher actualizado y vuelve a entrar."));
                continue;
            }
            if(p.getPos().squaredDistanceTo(session.anchor)>.04)p.teleport(p.getServerWorld(),session.anchor.x,session.anchor.y,session.anchor.z,180,0);
            p.setVelocity(Vec3d.ZERO);p.fallDistance=0;
        }
    }

    private static void send(ServerPlayerEntity p,Identifier channel,UUID id) {
        if(!ServerPlayNetworking.canSend(p,channel))return;
        PacketByteBuf b=PacketByteBufs.create();b.writeUuid(id==null?new UUID(0,0):id);
        if(channel.equals(DONE))b.writeBoolean(store!=null&&store.completed(p.getUuid()));
        ServerPlayNetworking.send(p,channel,b);
    }
}
