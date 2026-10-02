package net.tecnihardcore.mixin;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.entity.Entity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.VertexConsumerProvider;
import net.tecnihardcore.RitualVisuals;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(EntityRenderDispatcher.class)
public abstract class RitualEntityMixin {
    @Inject(method="render",at=@At("HEAD"),cancellable=true) private <E extends Entity> void hideDouble(E entity,double x,double y,double z,float yaw,float delta,MatrixStack m,VertexConsumerProvider consumers,int light,CallbackInfo ci){if(RitualVisuals.hideBody(entity.getUuid(),delta))ci.cancel();}
}
