package net.tecnihardcore;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.text.Text;
import java.nio.file.*;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

/** Client-owned HUD position. It is never replaced by launcher updates. */
public final class HudSettings {
    enum Anchor { TOP_RIGHT, TOP_CENTER, BOTTOM_LEFT, BOTTOM_RIGHT }
    private static final Path FILE=FabricLoader.getInstance().getConfigDir().resolve("tecnihardcore-hud.json");
    private static Anchor anchor=Anchor.TOP_RIGHT;

    public static void init(){
        load();
        ClientCommandRegistrationCallback.EVENT.register((dispatcher,registry)->dispatcher.register(literal("tecnihud")
            .executes(c->{show(c.getSource());return 1;})
            .then(literal("posicion")
                .then(literal("arriba-derecha").executes(c->set(c.getSource(),Anchor.TOP_RIGHT)))
                .then(literal("arriba-centro").executes(c->set(c.getSource(),Anchor.TOP_CENTER)))
                .then(literal("abajo-izquierda").executes(c->set(c.getSource(),Anchor.BOTTOM_LEFT)))
                .then(literal("abajo-derecha").executes(c->set(c.getSource(),Anchor.BOTTOM_RIGHT))))));
    }
    public static int x(int width,int panel){return switch(anchor){case TOP_RIGHT,BOTTOM_RIGHT->width-panel-12;case TOP_CENTER->(width-panel)/2;case BOTTOM_LEFT->12;};}
    public static int y(int height){return switch(anchor){case BOTTOM_LEFT,BOTTOM_RIGHT->height-82;case TOP_CENTER->42;case TOP_RIGHT->12;};}
    private static int set(net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource source,Anchor value){anchor=value;save();show(source);return 1;}
    private static void show(net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource source){source.sendFeedback(Text.literal("§6§l[TECNI HUD] §r§fPosición: "+label()+". Usa /tecnihud posicion y elige una opción."));}
    private static String label(){return switch(anchor){case TOP_RIGHT->"arriba-derecha";case TOP_CENTER->"arriba-centro";case BOTTOM_LEFT->"abajo-izquierda";case BOTTOM_RIGHT->"abajo-derecha";};}
    private static void load(){try{if(Files.exists(FILE)){var json=JsonParser.parseString(Files.readString(FILE)).getAsJsonObject();anchor=Anchor.valueOf(json.get("position").getAsString());}}catch(Exception e){Hardcore.LOG.warn("HUD config invalid; using top-right",e);anchor=Anchor.TOP_RIGHT;}}
    private static void save(){try{Files.createDirectories(FILE.getParent());JsonObject json=new JsonObject();json.addProperty("position",anchor.name());Path tmp=FILE.resolveSibling(FILE.getFileName()+".tmp");Files.writeString(tmp,json.toString());Files.move(tmp,FILE,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}catch(Exception e){Hardcore.LOG.error("Cannot save HUD position",e);}}
    private HudSettings(){}
}
