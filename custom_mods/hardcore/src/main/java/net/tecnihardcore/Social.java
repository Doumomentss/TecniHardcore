package net.tecnihardcore;

import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.text.Text;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.util.*;
import static net.minecraft.server.command.CommandManager.*;

public final class Social {
    public static SocialStore store;
    private record Invite(String team,long expires){}
    private static final Map<String,Invite> invites=new HashMap<>();
    private static final Set<String> privateChat=new HashSet<>();
    private static final Map<String,Integer> lastChat=new HashMap<>();
    public static SocialStore.Team team(String player){if(store==null)return null;return store.data.teams.values().stream().filter(t->t.members.contains(player)).findFirst().orElse(null);}
    static String teamId(String player){var t=team(player);return t==null?"":t.id;}
    public static void init(){
        ServerLifecycleEvents.SERVER_STARTED.register(s->store=new SocialStore(s));
        ServerLifecycleEvents.SERVER_STOPPING.register(s->{if(store!=null)store.save();});
        ServerLifecycleEvents.SERVER_STOPPED.register(s->{store=null;invites.clear();privateChat.clear();lastChat.clear();});
        ServerPlayConnectionEvents.DISCONNECT.register((h,s)->s.execute(()->{privateChat.remove(h.player.getUuidAsString());lastChat.remove(h.player.getUuidAsString());if(store!=null)store.save();}));
        ServerTickEvents.END_SERVER_TICK.register(s->{if(store==null||s.getTicks()%20!=0)return;boolean changed=false;for(var t:store.data.truces){var p=s.getPlayerManager().getPlayer(UUID.fromString(t.expelled));boolean online=p!=null&&AuthBootstrap.authenticated(p);if(online&&!t.notified){say(p,"Has salido o sido expulsado de "+t.name+". Protección mutua: "+((t.remaining+59)/60)+" minutos de conexión. Se pausa al salir.");t.notified=true;changed=true;}if(online&&t.remaining>0){t.remaining=SocialRules.remaining(t.remaining,true);if(t.remaining==0){say(p,"Terminó la protección tras tu salida de "+t.name+".");changed=true;}}}if(store.data.truces.removeIf(t->t.remaining<=0))changed=true;if(changed||s.getTicks()%1200==0)store.save();invites.values().removeIf(i->i.expires<System.currentTimeMillis());});
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity,source,damage)->{if(!(entity instanceof ServerPlayerEntity victim)||store==null)return true;var attacker=source.getAttacker();String aggressor=attacker instanceof ServerPlayerEntity p?p.getUuidAsString():attacker instanceof TameableEntity pet&&pet.getOwnerUuid()!=null?pet.getOwnerUuid().toString():"";return aggressor.isEmpty()||!SocialRules.protectedPair(aggressor,victim.getUuidAsString(),teamId(aggressor),teamId(victim.getUuidAsString()),store.data.truces);});
        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register((message,sender,parameters)->{if(!AuthBootstrap.authenticated(sender))return false;String id=sender.getUuidAsString();if(!privateChat.contains(id))return true;var t=team(id);if(t==null){privateChat.remove(id);say(sender,"Ya no pertenecés a un equipo. Chat general activado; vuelve a enviar el mensaje.");return false;}int tick=sender.getServer().getTicks();if(tick-lastChat.getOrDefault(id,-100)<5){say(sender,"Espera un momento antes de enviar otro mensaje.");return false;}lastChat.put(id,tick);Text text=Text.literal("§b[EQUIPO · "+t.name+"] §f"+sender.getName().getString()+": "+message.getContent().getString());for(String member:t.members){var p=sender.getServer().getPlayerManager().getPlayer(UUID.fromString(member));if(p!=null&&AuthBootstrap.authenticated(p))p.sendMessage(text,false);}return false;});
        CommandRegistrationCallback.EVENT.register((d,r,e)->{
            // Preserve vanilla administration, then replace its OP-only /team root.
            var vanilla=d.getRoot().getChild("team");if(vanilla!=null){var alias=literal("vanillateam").requires(s->s.hasPermissionLevel(2));for(var child:vanilla.getChildren())alias.then(child);d.register(alias);try{for(String field:new String[]{"children","literals","arguments"}){var f=com.mojang.brigadier.tree.CommandNode.class.getDeclaredField(field);f.setAccessible(true);((Map<?,?>)f.get(d.getRoot())).remove("team");}}catch(Exception error){throw new IllegalStateException("Cannot register player team commands",error);}}
            var root=literal("team").requires(s->s.getEntity() instanceof ServerPlayerEntity).executes(c->toggle(c.getSource().getPlayerOrThrow()));
            root.then(literal("crear").then(argument("nombre",StringArgumentType.word()).executes(c->create(c.getSource().getPlayerOrThrow(),StringArgumentType.getString(c,"nombre")))));
            root.then(literal("invitar").then(argument("jugador",StringArgumentType.word()).executes(c->invite(c.getSource().getPlayerOrThrow(),StringArgumentType.getString(c,"jugador")))));
            root.then(literal("aceptar").executes(c->accept(c.getSource().getPlayerOrThrow())));
            root.then(literal("expulsar").then(argument("jugador",StringArgumentType.word()).executes(c->kick(c.getSource().getPlayerOrThrow(),StringArgumentType.getString(c,"jugador")))));
            root.then(literal("salir").executes(c->leave(c.getSource().getPlayerOrThrow())));
            root.then(literal("info").executes(c->info(c.getSource().getPlayerOrThrow())));
            root.then(literal("transferir").then(argument("jugador",StringArgumentType.word()).executes(c->transfer(c.getSource().getPlayerOrThrow(),StringArgumentType.getString(c,"jugador")))));d.register(root);
            d.register(literal("chat").requires(s->s.getEntity() instanceof ServerPlayerEntity).executes(c->toggle(c.getSource().getPlayerOrThrow())).then(literal("general").executes(c->channel(c.getSource().getPlayerOrThrow(),false))).then(literal("team").executes(c->channel(c.getSource().getPlayerOrThrow(),true))));
        });
    }
    static boolean ready(ServerPlayerEntity p){if(store==null||!AuthBootstrap.authenticated(p)){say(p,"Primero debes autenticarte con /login o /register.");return false;}return true;}
    public static void say(ServerPlayerEntity p,String text){p.sendMessage(Text.literal(text),false);}
    static int channel(ServerPlayerEntity p,boolean team){if(!ready(p))return 0;if(team&&team(p.getUuidAsString())==null){say(p,"No pertenecés a un equipo. Usa /team crear Nombre.");return 0;}if(team)privateChat.add(p.getUuidAsString());else privateChat.remove(p.getUuidAsString());say(p,team?"Chat privado del equipo ACTIVADO. /chat vuelve al general.":"Chat GENERAL activado.");return 1;}
    static int toggle(ServerPlayerEntity p){return channel(p,!privateChat.contains(p.getUuidAsString()));}
    static int create(ServerPlayerEntity p,String name){if(!ready(p))return 0;if(!SocialRules.teamName(name)||team(p.getUuidAsString())!=null||store.data.teams.values().stream().anyMatch(t->t.name.equalsIgnoreCase(name))){say(p,"Nombre ocupado o inválido (3–20 letras, números o _), o ya pertenecés a un equipo.");return 0;}var b=store.copy();var t=new SocialStore.Team();t.id=UUID.randomUUID().toString();t.name=name;t.owner=p.getUuidAsString();t.members.add(t.owner);b.teams.put(t.id,t);store.commit(b,"create team "+t.id);say(p,"Equipo "+name+" creado. /team invitar Nombre · /team activa el chat privado.");return 1;}
    static SocialStore.Team owned(ServerPlayerEntity p){if(!ready(p))return null;var t=team(p.getUuidAsString());if(t==null||!t.owner.equals(p.getUuidAsString())){say(p,"Solo el dueño del equipo puede hacerlo.");return null;}return t;}
    static int invite(ServerPlayerEntity p,String name){var t=owned(p);if(t==null)return 0;var other=p.getServer().getPlayerManager().getPlayer(name);if(other==null||!AuthBootstrap.authenticated(other)||team(other.getUuidAsString())!=null||t.members.size()>=12){say(p,"Jugador no disponible, ya tiene equipo o el equipo está completo (12).");return 0;}invites.put(other.getUuidAsString(),new Invite(t.id,System.currentTimeMillis()+300000));say(other,p.getName().getString()+" te invita a "+t.name+". /team aceptar (caduca en 5 minutos).");say(p,"Invitación enviada.");return 1;}
    static int accept(ServerPlayerEntity p){if(!ready(p))return 0;var i=invites.remove(p.getUuidAsString());var t=i==null?null:store.data.teams.get(i.team);if(t==null||i.expires<System.currentTimeMillis()||team(p.getUuidAsString())!=null||t.members.size()>=12){say(p,"No hay una invitación vigente.");return 0;}var b=store.copy();b.teams.get(t.id).members.add(p.getUuidAsString());store.commit(b,"join team "+t.id);say(p,"Entraste a "+t.name+". No hay daño entre compañeros.");return 1;}
    static String member(SocialStore.Team t,String name,ServerPlayerEntity p){return t.members.stream().filter(id->p.getServer().getUserCache().getByUuid(UUID.fromString(id)).map(profile->profile.getName().equalsIgnoreCase(name)).orElse(false)).findFirst().orElse(null);}
    static void truce(SocialStore.Business b,SocialStore.Team t,String expelled){var v=new SocialStore.Truce();v.expelled=expelled;v.team=t.id;v.name=t.name;v.members.addAll(t.members);v.members.remove(expelled);b.truces.add(v);}
    static int kick(ServerPlayerEntity p,String name){var t=owned(p);if(t==null)return 0;String id=member(t,name,p);if(id==null||id.equals(t.owner)){say(p,"Ese jugador no es un miembro expulsable.");return 0;}var b=store.copy();truce(b,t,id);b.teams.get(t.id).members.remove(id);store.commit(b,"kick "+id+" from "+t.id);privateChat.remove(id);say(p,"Expulsado. Protección mutua durante 2 horas de conexión del expulsado.");var other=p.getServer().getPlayerManager().getPlayer(UUID.fromString(id));if(other!=null)say(other,"Fuiste expulsado de "+t.name+". Chat general activado; recibirás los detalles de protección al autenticarte.");return 1;}
    static int leave(ServerPlayerEntity p){if(!ready(p))return 0;var t=team(p.getUuidAsString());if(t==null)return 0;if(t.owner.equals(p.getUuidAsString())&&t.members.size()>1){say(p,"Transfiere el liderazgo con /team transferir Nombre antes de salir.");return 0;}var b=store.copy();if(t.members.size()>1)truce(b,t,p.getUuidAsString());b.teams.get(t.id).members.remove(p.getUuidAsString());if(b.teams.get(t.id).members.isEmpty())b.teams.remove(t.id);store.commit(b,"leave team "+t.id);privateChat.remove(p.getUuidAsString());say(p,"Saliste del equipo. Chat general activado.");return 1;}
    static int transfer(ServerPlayerEntity p,String name){var t=owned(p);if(t==null)return 0;String id=member(t,name,p);if(id==null)return 0;var b=store.copy();b.teams.get(t.id).owner=id;store.commit(b,"transfer team "+t.id);say(p,"Liderazgo transferido a "+name+".");return 1;}
    static int info(ServerPlayerEntity p){if(!ready(p))return 0;var t=team(p.getUuidAsString());say(p,t==null?"No pertenecés a un equipo.":"Equipo "+t.name+" · "+t.members.size()+"/12 miembros · "+(privateChat.contains(p.getUuidAsString())?"chat privado":"chat general"));for(var v:store.data.truces)if(v.expelled.equals(p.getUuidAsString())||v.members.contains(p.getUuidAsString()))say(p,"Protección con "+v.name+": "+((v.remaining+59)/60)+" minutos de conexión del expulsado.");return 1;}
    private Social(){}
}
