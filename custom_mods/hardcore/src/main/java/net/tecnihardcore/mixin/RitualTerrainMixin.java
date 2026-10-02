package net.tecnihardcore.mixin;
import net.minecraft.client.render.WorldRenderer;
import net.tecnihardcore.RitualVisuals;
import com.mojang.blaze3d.systems.RenderSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(WorldRenderer.class)
public abstract class RitualTerrainMixin {
    @Inject(method="renderLayer",at=@At("HEAD")) private void darken(CallbackInfo ci){float light=(float)(1-RitualVisuals.darkness());RenderSystem.setShaderColor(light,light,light,1);}
    @Inject(method="renderLayer",at=@At("RETURN")) private void restore(CallbackInfo ci){RenderSystem.setShaderColor(1,1,1,1);}
    @Inject(method="renderClouds",at=@At("HEAD")) private void darkCloud(CallbackInfo ci){float light=(float)(1-RitualVisuals.darkness());RenderSystem.setShaderColor(light,light,light,1);}
    @Inject(method="renderClouds",at=@At("RETURN")) private void restoreCloud(CallbackInfo ci){RenderSystem.setShaderColor(1,1,1,1);}
}
