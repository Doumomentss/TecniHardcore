package net.tecnihardcore;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.loader.api.FabricLoader;

/** Exemptions originate in server gameplay state, never in a client request. */
public final class SecurityBridge {
    private static final Map<UUID,Long> authorized=new ConcurrentHashMap<>();
    public static void init(){
        ServerGuard.init();
        net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT.register((d,r,e)->{
            var root=net.minecraft.server.command.CommandManager.literal("seguridad").requires(s->s.hasPermissionLevel(4));
            root.then(net.minecraft.server.command.CommandManager.literal("estado").executes(c->{c.getSource().sendFeedback(()->net.minecraft.text.Text.literal("AntiXray: "+FabricLoader.getInstance().isModLoaded("antixray")+" · Fiw: "+FabricLoader.getInstance().isModLoaded("fiw-mods-api")+" · TecniGuard: activo · Grim experimental: "+FabricLoader.getInstance().isModLoaded("grimac")),false);return 1;}));
            root.then(net.minecraft.server.command.CommandManager.literal("alertas").executes(c->{var p=c.getSource().getPlayerOrThrow();boolean enabled;if(ServerGuard.alerts.remove(p.getUuid()))enabled=false;else{ServerGuard.alerts.add(p.getUuid());enabled=true;}Social.say(p,"Alertas TecniGuard "+(enabled?"ACTIVADAS":"desactivadas"));return 1;}));
            root.then(net.minecraft.server.command.CommandManager.literal("revisar").then(net.minecraft.server.command.CommandManager.argument("jugador",com.mojang.brigadier.arguments.StringArgumentType.word()).executes(c->{var p=c.getSource().getServer().getPlayerManager().getPlayer(com.mojang.brigadier.arguments.StringArgumentType.getString(c,"jugador"));if(p==null)return 0;c.getSource().sendFeedback(()->net.minecraft.text.Text.literal(p.getName().getString()+" · "+ServerGuard.summary(p.getUuid())),false);return 1;})));
            d.register(net.minecraft.server.command.CommandManager.literal("tecni").then(root));
        });
        if(!FabricLoader.getInstance().isModLoaded("grimac"))return;
        ServerLifecycleEvents.SERVER_STARTED.register(s->GrimHooks.start());
        ServerTickEvents.END_SERVER_TICK.register(s->{
            long now=System.currentTimeMillis();
            for(var p:s.getPlayerManager().getPlayerList()){
                boolean special=!AuthBootstrap.authenticated(p)||p.getVehicle() instanceof CrystalMount||p.hasStatusEffect(BossRoots.ROOT)||Rituals.participant(p.getUuid());
                if(!special)for(var h:Cataclysms.active.values())if(h.world==p.getWorld()&&h.age>=200&&!h.stopping&&(h.type==0||h.type==1)&&p.getPos().subtract(net.minecraft.util.math.Vec3d.ofCenter(h.center)).horizontalLength()<h.radius+12){special=true;break;}
                if(special)authorized.put(p.getUuid(),now+2500);
            }
            if(s.getTicks()%100==0)authorized.entrySet().removeIf(e->e.getValue()<now);
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(s->{GrimHooks.stop();authorized.clear();});
    }
    private static final class GrimHooks {
        static ac.grim.grimac.api.GrimAbstractAPI api;
        static void start(){
            api=ac.grim.grimac.api.GrimAPIProvider.get();
            if(api==null)throw new IllegalStateException("Grim API missing: cannot protect custom movements");
            api.getEventBus().subscribe("tecnihardcore",ac.grim.grimac.api.event.events.FlagEvent.class,event->{
                String check=event.getCheck().getCheckName();
                boolean movement=check.matches("(?i).*(simulation|ground|nofall|noslow|knockback|explosion|vehicle|phase|sprint|elytra).*" );
                if(movement&&authorized.getOrDefault(event.getUser().getUniqueId(),0L)>System.currentTimeMillis())event.setCancelled(true);
            });
            Hardcore.LOG.info("Grim integration ready: server-authorized custom movements exempted, alerts enabled");
        }
        static void stop(){if(api!=null)api.getEventBus().unregisterAllListeners("tecnihardcore");api=null;}
    }
    private SecurityBridge(){}
}
