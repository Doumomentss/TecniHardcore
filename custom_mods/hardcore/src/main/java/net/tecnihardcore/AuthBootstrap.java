package net.tecnihardcore;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import xyz.nikitacartes.easyauth.EasyAuth;
import xyz.nikitacartes.easyauth.utils.PlayerAuth;
import xyz.nikitacartes.easyauth.storage.PlayerEntryV1;
import xyz.nikitacartes.easyauth.utils.AuthHelper;
import java.nio.file.*;
import java.security.*;
import java.util.*;

/** Standard register/login, with local first registration for pre-existing identities. */
public final class AuthBootstrap {
    private static Path pendingFile;
    private static Map<String,String> pending = new HashMap<>();
    public static boolean enabled() { return FabricLoader.getInstance().isModLoaded("easyauth"); }
    public static boolean serverBot(ServerPlayerEntity p) {
        if(!FabricLoader.getInstance().isModLoaded("carpet"))return false;
        try{return Class.forName("carpet.patches.EntityPlayerMPFake").isInstance(p);}
        catch(ClassNotFoundException e){return false;}
    }
    public static boolean authenticated(ServerPlayerEntity p) { return serverBot(p) || !enabled() || ((PlayerAuth)p).easyAuth$isAuthenticated(); }
    public static void start(MinecraftServer s) {
        if(!enabled()) {
            if(!Boolean.getBoolean("tecni.testServer"))throw new IllegalStateException("EasyAuth is required on the production server");
            return;
        }
        EasyAuth.config.premiumAutoLogin=false; EasyAuth.config.offlineByDefault=true; EasyAuth.config.sessionTimeout=0;
        EasyAuth.extendedConfig.forcedOfflineUuid=true; EasyAuth.extendedConfig.allowCaseInsensitiveUsername=false;
        EasyAuth.extendedConfig.skipAllAuthChecks=false;
        EasyAuth.extendedConfig.preventOfflinePlayersWithOnlineUsernames=false;
        EasyAuth.extendedConfig.allowCommands=false; EasyAuth.extendedConfig.playerInvulnerable=true;
        EasyAuth.extendedConfig.minPasswordLength=12;
        EasyAuth.extendedConfig.maxPasswordLength=128;
        EasyAuth.langConfig.loginRequired=new xyz.nikitacartes.easyauth.config.LangConfigV1.TranslatableText("tecnihardcore.auth.login_required","Primera vez: /register CLAVE CLAVE (mínimo 12 caracteres). Si ya te registraste: /login CLAVE.",true,false);
        EasyAuth.langConfig.registerRequired=new xyz.nikitacartes.easyauth.config.LangConfigV1.TranslatableText("tecnihardcore.auth.register_required","Regístrate con /register CLAVE CLAVE. Usa entre 12 y 128 caracteres y repite la misma contraseña.",true,false);
        EasyAuth.saveConfigs();
        pendingFile=s.getRunDirectory().toPath().resolve("config/tecnihardcore/pending-claims.json");
        try {
            if(Files.exists(pendingFile)) {
                var json=JsonParser.parseString(Files.readString(pendingFile)).getAsJsonObject(); json.entrySet().forEach(e->pending.put(e.getKey(),e.getValue().getAsString()));
            }
            Path cache=s.getRunDirectory().toPath().resolve("usercache.json");
            Path reserved=s.getRunDirectory().toPath().resolve("config/tecnihardcore/reserved-identities.json");
            if(!Files.exists(reserved)) {
                Files.createDirectories(reserved.getParent());
                Files.writeString(reserved,Files.exists(cache)?Files.readString(cache):"[]",StandardOpenOption.CREATE_NEW);
            }
            for(JsonElement v:JsonParser.parseString(Files.readString(reserved)).getAsJsonArray()) {
                JsonObject profile=v.getAsJsonObject(); String name=profile.get("name").getAsString();
                PlayerEntryV1 entry=EasyAuth.DB.getUserData(name);
                String key=name.toLowerCase(Locale.ROOT);
                if(entry!=null && entry.password!=null && !entry.password.isEmpty()) {
                    if(pending.containsKey(key)) {
                        String marker="local-register:"+hash(entry.password);
                        if(pending.get(key).startsWith("local-register:") && !pending.get(key).equals(marker))pending.remove(key);
                        else pending.put(key,marker);
                    }
                    continue;
                }
                boolean existed=entry!=null;
                // EasyAuth 3.3.6's two-argument constructor leaves usernames null.
                entry=new PlayerEntryV1(name);
                entry.uuid=UUID.fromString(profile.get("uuid").getAsString());
                entry.password=AuthHelper.hashPassword(randomCode().toCharArray());
                if(existed)EasyAuth.DB.updateUserData(entry);else EasyAuth.DB.registerUser(entry);
                PlayerEntryV1 registered=EasyAuth.DB.getUserData(name);
                if(registered==null || !entry.password.equals(registered.password) || !entry.uuid.equals(registered.uuid))
                    throw new IllegalStateException("Account reservation failed verification");
                pending.put(key,"local-register:"+hash(entry.password));
            }
            savePending();
            Hardcore.LOG.info("Standard /register and /login ready; existing identities retain local registration protection");
        }catch(Exception e){throw new IllegalStateException("Cannot protect existing accounts",e);}
    }
    private static String randomCode(){byte[] b=new byte[24];new SecureRandom().nextBytes(b);return Base64.getUrlEncoder().withoutPadding().encodeToString(b);}
    private static String hash(String value) throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));}
    private static void savePending() throws Exception {
        Files.createDirectories(pendingFile.getParent());Path tmp=pendingFile.resolveSibling("pending-claims.tmp");
        Files.writeString(tmp,SoulStore.JSON.toJson(pending));Files.move(tmp,pendingFile,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
    }
    public static boolean intercept(ServerPlayerEntity p,String command) {
        if(!enabled())return false;
        String[] args=command.trim().split("\\s+");
        String action=args[0].substring(args[0].lastIndexOf(':')+1);
        if(action.equals("activar")) {p.sendMessage(Text.literal("Usa /register CLAVE CLAVE la primera vez y /login CLAVE al volver a entrar."),false);return true;}
        if(!action.equals("register") && !action.equals("reg"))return false;
        String name=p.getGameProfile().getName().toLowerCase(Locale.ROOT);
        if(!pending.containsKey(name))return false;
        try {
            PlayerAuth auth=(PlayerAuth)p;
            if(!AuthRules.localRegistration(auth.easyAuth$getIpAddress())) {
                p.sendMessage(Text.literal("Este nombre pertenece a un jugador existente. Su primer registro debe hacerse desde la PC del servidor."),false);return true;
            }
            String error=AuthRules.registrationError(args);
            if(error!=null) {
                p.sendMessage(Text.literal(error),false);
                Hardcore.LOG.info("Registration validation rejected for {}: argument count {}, password length {}, repeat matches {}",p.getUuid(),args.length,args.length>1?args[1].length():0,args.length==3 && args[1].equals(args[2]));
                return true;
            }
            PlayerEntryV1 entry=EasyAuth.DB.getUserData(p.getGameProfile().getName());
            entry.password=AuthHelper.hashPassword(args[1].toCharArray());
            entry.registrationDate=java.time.ZonedDateTime.now();
            EasyAuth.DB.updateUserData(entry);
            if(!entry.password.equals(EasyAuth.DB.getUserData(p.getGameProfile().getName()).password))throw new IllegalStateException("Account update failed verification");
            auth.easyAuth$setPlayerEntryV1(entry);
            pending.remove(name);savePending();
            p.sendMessage(Text.literal("Registro completado. Ahora usa /login CLAVE."),false);
            Hardcore.LOG.info("Existing account registered locally: {}",p.getUuid());
        }catch(Exception e){Hardcore.LOG.error("Account registration failed for {}",p.getUuid());p.networkHandler.disconnect(Text.literal("No se pudo registrar la cuenta. Contacta al administrador."));}
        return true;
    }

    public static void welcome(ServerPlayerEntity p) {
        if(serverBot(p)) {
            if(enabled()) {
                PlayerAuth auth=(PlayerAuth)p;
                auth.easyAuth$setSkipAuth();
                auth.easyAuth$setAuthenticated(true);
                auth.easyAuth$setKickTimer(Long.MAX_VALUE);
            }
            Hardcore.LOG.info("Server-created Carpet bot authenticated: {}",p.getName().getString());
            return;
        }
        if(enabled() && pending.containsKey(p.getGameProfile().getName().toLowerCase(Locale.ROOT)))
            p.sendMessage(Text.literal("Registra tu cuenta con /register CLAVE CLAVE y entra con /login CLAVE. El primer registro de tu identidad requiere conexión local."),false);
    }
}
