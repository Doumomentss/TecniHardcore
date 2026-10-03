package net.tecnihardcore;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.ScreenshotRecorder;
import java.util.*;
/** Opt-in framebuffer capture for reproducible visual tests, inactive in normal launches. */
public final class QaCapture {
    private static int ticks,screenTicks;private static UUID ritualId;private static final Set<String> captured=new HashSet<>();
    public static void frame(MinecraftClient c){
        if(!Boolean.getBoolean("tecni.visualTest")||c.world==null||c.player==null)return;
        c.options.pauseOnLostFocus=false;
        if(c.currentScreen instanceof net.minecraft.client.gui.screen.GameMenuScreen)c.setScreen(null);
        ticks++;
        if(ticks%30==0)try{
            var file=c.runDirectory.toPath().resolve("qa-action.json");if(java.nio.file.Files.exists(file)){
                var action=com.google.gson.JsonParser.parseString(java.nio.file.Files.readString(file)).getAsJsonObject();java.nio.file.Files.delete(file);
                switch(action.get("type").getAsString()){
                    case "altar"->{var pos=new net.minecraft.util.math.BlockPos(3,97,0);c.interactionManager.interactBlock(c.player,net.minecraft.util.Hand.MAIN_HAND,new net.minecraft.util.hit.BlockHitResult(net.minecraft.util.math.Vec3d.ofCenter(pos),net.minecraft.util.math.Direction.EAST,pos,false));}
                    case "use"->c.interactionManager.interactItem(c.player,net.minecraft.util.Hand.MAIN_HAND);
                    case "inventory"->c.setScreen(new net.minecraft.client.gui.screen.ingame.InventoryScreen(c.player));
                    case "close"->c.setScreen(null);
                }
            }
        }catch(Exception e){Hardcore.LOG.warn("QA action failed: {}",e.getClass().getSimpleName());}
        if(ticks%30==0)try{var request=c.runDirectory.toPath().resolve("qa-capture-request.txt");if(java.nio.file.Files.exists(request)){String phase=java.nio.file.Files.readString(request).trim();java.nio.file.Files.delete(request);if(phase.equals("graphics-reload")){c.options.load();var iris=Class.forName("net.irisshaders.iris.Iris");var config=iris.getMethod("getIrisConfig").invoke(null);config.getClass().getMethod("load").invoke(config);iris.getMethod("reload").invoke(null);c.worldRenderer.reload();Hardcore.LOG.info("QA graphics reloaded");}else if(phase.equals("fullscreen-toggle")){c.getWindow().toggleFullscreen();}else if(phase.equals("board-open")){var pos=new net.minecraft.util.math.BlockPos(6,96,-17);c.interactionManager.interactBlock(c.player,net.minecraft.util.Hand.MAIN_HAND,new net.minecraft.util.hit.BlockHitResult(net.minecraft.util.math.Vec3d.ofCenter(pos),net.minecraft.util.math.Direction.NORTH,pos,false));}else if(phase.equals("board-scale4")){c.options.getGuiScale().setValue(4);c.onResolutionChanged();}else if(phase.equals("board-close")){c.setScreen(null);c.options.getGuiScale().setValue(2);c.onResolutionChanged();}else if(phase.matches("[a-z0-9-]{1,50}")){captured.remove(phase);capture(c,phase);}}}catch(Exception e){Hardcore.LOG.warn("QA request failed: {}",e.getClass().getSimpleName());}
        if(Boolean.getBoolean("tecni.bossVisual")){
            if(ticks%60==0&&java.nio.file.Files.exists(c.runDirectory.toPath().resolve("qa-altar-open"))){try{java.nio.file.Files.delete(c.runDirectory.toPath().resolve("qa-altar-open"));var pos=new net.minecraft.util.math.BlockPos(3,97,0);c.interactionManager.interactBlock(c.player,net.minecraft.util.Hand.MAIN_HAND,new net.minecraft.util.hit.BlockHitResult(net.minecraft.util.math.Vec3d.ofCenter(pos),net.minecraft.util.math.Direction.EAST,pos,false));}catch(Exception e){Hardcore.LOG.warn("QA altar interaction failed");}}
            if(ticks==180){c.options.getGuiScale().setValue(2);c.onResolutionChanged();}
            if(ticks==220)capture(c,"boss-overview");
            for(var b:c.world.getEntities())if(b instanceof TrialBoss boss){
                if(boss.transition()>0)capture(c,"phase-"+boss.phase()+"-transform-"+(boss.transition()>80?"fracture":boss.transition()>40?"rebuild":"complete"));
                if(boss.dying()>15)capture(c,"boss-death-"+(boss.dying()<40?"collapse":boss.dying()<85?"fracture":"dissipation"));
                if(boss.attack()!=0&&boss.attackAge()>15&&boss.attackAge()<28)capture(c,"boss-attack-"+boss.attack()+"-phase-"+boss.phase());
            }
            return;
        }
        if(ticks==180){c.options.getGuiScale().setValue(2);c.onResolutionChanged();}
        if(ticks==220)capture(c,"idle-scale2");
        if(ticks==260)c.getWindow().toggleFullscreen();
        if(ticks==310)capture(c,"idle-fullscreen");
        if(ticks==350)c.getWindow().toggleFullscreen();
        if(ticks>400&&ticks%240==0&&c.currentScreen==null&&c.player.getMainHandStack().isOf(Hardcore.HEART)&&!captured.contains("selection-ready")) {
            for(var p:net.minecraft.util.math.BlockPos.iterate(c.player.getBlockPos().add(-4,-2,-4),c.player.getBlockPos().add(4,2,4)))if(c.world.getBlockState(p).isOf(Sanctuaries.CORE)){
                captured.add("selection-request");var b=net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create();b.writeBlockPos(p);net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(RitualNetwork.QUERY,b);break;
            }
        }
        if(c.currentScreen instanceof SanctuaryScreen){
            screenTicks++;if(screenTicks==20){captured.remove("selection-scale2");capture(c,"selection-scale2");if(((SanctuaryScreen)c.currentScreen).hasCandidates())captured.add("selection-ready");}
            if(screenTicks==35){c.options.getGuiScale().setValue(4);c.onResolutionChanged();}
            if(screenTicks==45){for(var button:c.currentScreen.children())if(button instanceof net.minecraft.client.gui.widget.ButtonWidget w&&w.getMessage().getString().contains("resurrecciones")){w.onPress();break;}}
            if(screenTicks==55){captured.remove("selection-scale4");capture(c,"selection-scale4");}
            if(screenTicks==65){for(var button:c.currentScreen.children())if(button instanceof net.minecraft.client.gui.widget.ButtonWidget w&&w.active&&w.getMessage().getString().equals("INICIAR RESURRECCIÓN")){w.onPress();break;}}
            if(screenTicks==70){c.options.getGuiScale().setValue(2);c.onResolutionChanged();c.setScreen(null);}
        }else screenTicks=0;
        var v=RitualVisuals.nearest();if(v!=null){double age=v.elapsed(0);
            if(!v.id.equals(ritualId)){ritualId=v.id;captured.removeIf(s->!s.startsWith("selection")&&!s.startsWith("idle"));}
            String prefix=v.target.equals(c.player.getUuid())?"target-":"";
            if(age>20&&age<80)capture(c,prefix+"awakening");if(age>140&&age<280)capture(c,prefix+"extraction");if(age>340&&age<520)capture(c,prefix+"reconstruction");if(age>550&&age<600)capture(c,prefix+"culmination");if(v.state==1)capture(c,prefix+"restored");
            if(age>205&&age<240)capture(c,prefix+"cinema-start");if(age>630&&age<650)capture(c,prefix+"descent");if(age>670&&age<680)capture(c,prefix+"landing");
        }else if(ritualId!=null){capture(c,"camera-restored");ritualId=null;
        }
    }
    private static void capture(MinecraftClient c,String phase){if(captured.add(phase))ScreenshotRecorder.saveScreenshot(c.runDirectory,"sanctuary-"+phase+".png",c.getFramebuffer(),t->Hardcore.LOG.info("QA framebuffer: {} ({} FPS)",phase,c.getCurrentFps()));}
}
