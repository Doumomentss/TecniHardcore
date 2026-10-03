package net.tecnihardcore;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.*;
import net.fabricmc.fabric.api.client.rendering.v1.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
public final class ExpansionClient {
    private static final KeyBinding UP=new KeyBinding("key.tecnihardcore.mount_up",InputUtil.Type.KEYSYM,GLFW.GLFW_KEY_SPACE,"category.tecnihardcore");
    private static final KeyBinding DOWN=new KeyBinding("key.tecnihardcore.mount_down",InputUtil.Type.KEYSYM,GLFW.GLFW_KEY_Z,"category.tecnihardcore");
    private static final KeyBinding BOOST=new KeyBinding("key.tecnihardcore.mount_boost",InputUtil.Type.KEYSYM,GLFW.GLFW_KEY_R,"category.tecnihardcore");
    private static int qaTicks;private static float qaForward,qaSide,qaVertical;private static boolean qaBoost;
    static void qaDrive(float forward,float side,float vertical,boolean boost,int ticks){if(!Boolean.getBoolean("tecni.expansionVisual"))return;qaForward=forward;qaSide=side;qaVertical=vertical;qaBoost=boost;qaTicks=ticks;}
    public static void init(){
        KeyBindingHelper.registerKeyBinding(UP);KeyBindingHelper.registerKeyBinding(DOWN);KeyBindingHelper.registerKeyBinding(BOOST);
        EntityRendererRegistry.register(CrystalMount.TYPE,CrystalMountRenderer::new);CataclysmVisuals.init();EventPresentation.init();
        ClientTickEvents.END_CLIENT_TICK.register(c->{if(c.player!=null&&c.player.getVehicle() instanceof CrystalMount m&&ClientPlayNetworking.canSend(CrystalMount.INPUT)){var b=net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create();b.writeVarInt(m.getId());b.writeFloat(c.currentScreen==null?(qaTicks>0?qaForward:c.player.input.movementForward):0);b.writeFloat(c.currentScreen==null?(qaTicks>0?qaSide:c.player.input.movementSideways):0);b.writeFloat(c.currentScreen==null?(qaTicks>0?qaVertical:UP.isPressed()?1:DOWN.isPressed()?-1:0):0);b.writeBoolean(c.currentScreen==null&&(qaTicks>0?qaBoost:BOOST.isPressed()));ClientPlayNetworking.send(CrystalMount.INPUT,b);if(qaTicks>0)qaTicks--;}});
        HudRenderCallback.EVENT.register((draw,delta)->{var c=MinecraftClient.getInstance();if(c.player==null||c.options.hudHidden||!(c.player.getVehicle() instanceof CrystalMount m))return;draw.drawCenteredTextWithShadow(c.textRenderer,ExpansionRules.MOUNTS[m.tier()-1]+" · "+(int)m.getHealth()+" / "+(int)m.getMaxHealth()+" · "+(m.state()==2?"IMPULSO":"VUELO"),draw.getScaledWindowWidth()/2,draw.getScaledWindowHeight()-78,0x8cdbed);});
    }
}
