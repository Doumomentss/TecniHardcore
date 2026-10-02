package net.tecnihardcore;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.*;
import net.minecraft.world.RaycastContext;
import net.minecraft.util.hit.HitResult;
import java.util.UUID;

/** Render-only camera: the player's position, perspective preference and rotation stay intact. */
public final class RitualCinema {
    private static UUID skipped;
    public static RitualVisuals.Visual current(){
        var c=MinecraftClient.getInstance();if(c.player==null||c.world==null)return null;
        var v=RitualVisuals.nearest();if(v==null||!CinematicRules.cameraTime(v.elapsed(0))||v.id.equals(skipped))return null;
        if(!v.caster.equals(c.player.getUuid())&&!v.target.equals(c.player.getUuid()))return null;
        if(c.currentScreen!=null){skipped=v.id;return null;}
        return v;
    }
    public static void clear(){skipped=null;}
    public static Vec3d[] pose(RitualVisuals.Visual v,float delta){
        var c=MinecraftClient.getInstance();double age=v.elapsed(delta);
        Vec3d core=Vec3d.ofBottomCenter(v.altar),anchor=v.anchor;
        var caster=c.world.getPlayerByUuid(v.caster);if(caster!=null)anchor=caster.getLerpedPos(delta);
        Vec3d direction=core.subtract(anchor).multiply(1,0,1).normalize();if(direction.lengthSquared()<.01)direction=new Vec3d(0,0,1);
        double lift=CinematicRules.ease((age-200)/320),down=CinematicRules.descent(age);
        Vec3d position=anchor.subtract(direction.multiply(11.5)).add(-direction.z*1.2,5+lift*3*(1-down),direction.x*1.2);
        Vec3d focus=core.add(0,4.8+lift*1.5,0);
        if(age>=520){Vec3d scene=anchor.add(0,1.6,0).lerp(RitualVisuals.actorPosition(v,age).add(0,1,0),.6);focus=focus.lerp(scene,CinematicRules.ease((age-520)/80));}
        Vec3d eye=anchor.add(0,1.6,0);var hit=c.world.raycast(new RaycastContext(eye,position,RaycastContext.ShapeType.VISUAL,RaycastContext.FluidHandling.NONE,c.player));
        if(hit.getType()!=HitResult.Type.MISS){
            for(double angle:new double[]{.8,-.8,1.3,-1.3}){
                Vec3d alternative=anchor.subtract(direction.rotateY((float)angle).multiply(11.5)).add(0,5+lift*3*(1-down),0);
                var test=c.world.raycast(new RaycastContext(eye,alternative,RaycastContext.ShapeType.VISUAL,RaycastContext.FluidHandling.NONE,c.player));
                if(test.getType()==HitResult.Type.MISS){position=alternative;hit=test;break;}
            }
        }
        if(hit.getType()!=HitResult.Type.MISS)position=hit.getPos().add(eye.subtract(position).normalize().multiply(.3));
        return new Vec3d[]{position,focus};
    }
    public static void hud(DrawContext d,float delta){
        var v=current();if(v==null)return;var c=MinecraftClient.getInstance();int w=d.getScaledWindowWidth(),h=d.getScaledWindowHeight(),bar=Math.max(16,h/12);
        d.fill(0,0,w,bar,0xee03060b);d.fill(0,h-bar,w,h,0xee03060b);
        double age=v.elapsed(delta);String stage=age<280?"EL ALMA DESPIERTA":age<520?"EL CRISTAL RECONSTRUYE SU ESENCIA":age<600?"REGRESO AL MUNDO":"EL ALMA HA REGRESADO";
        d.drawCenteredTextWithShadow(c.textRenderer,stage+" · "+v.name,w/2,bar+8,0xa9deff);
        int length=Math.min(240,w-40),left=(w-length)/2;
        d.fill(left,h-bar-12,left+length,h-bar-9,0xaa132839);d.fill(left,h-bar-12,left+(int)(length*Math.min(1,age/600)),h-bar-9,0xff70c8ff);
        d.drawCenteredTextWithShadow(c.textRenderer,"Esc: recuperar cámara",w/2,h-bar+(bar-8)/2,0x8296a7);
    }
}
