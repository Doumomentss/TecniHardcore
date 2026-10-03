package net.tecnihardcore.mixin;
import net.minecraft.client.render.Camera;
import net.minecraft.world.BlockView;
import net.minecraft.entity.Entity;
import net.tecnihardcore.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Camera.class)
public abstract class RitualCameraMixin {
    @Shadow private boolean thirdPerson;
    @Shadow protected abstract void setRotation(float yaw,float pitch);
    @Shadow protected abstract void setPos(double x,double y,double z);
    @Inject(method="update",at=@At("TAIL"))
    private void cinema(BlockView world,Entity entity,boolean third,boolean inverse,float delta,CallbackInfo ci){
        float shake=CataclysmVisuals.shake(delta);if(shake!=0){var camera=(Camera)(Object)this;setRotation(camera.getYaw()+shake,camera.getPitch()+shake*.5F);}
        var v=RitualCinema.current();if(v==null)return;var pose=RitualCinema.pose(v,delta);var d=pose[1].subtract(pose[0]);
        setRotation((float)Math.toDegrees(Math.atan2(-d.x,d.z)),(float)-Math.toDegrees(Math.atan2(d.y,Math.sqrt(d.x*d.x+d.z*d.z))));
        setPos(pose[0].x,pose[0].y,pose[0].z);thirdPerson=true;
    }
}
