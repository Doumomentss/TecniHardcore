package net.tecnihardcore.mixin;
import net.minecraft.client.render.*;
import net.minecraft.client.world.ClientWorld;
import net.tecnihardcore.RitualVisuals;
import com.mojang.blaze3d.systems.RenderSystem;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(BackgroundRenderer.class)
public abstract class RitualFogMixin {
    @Shadow private static float red,green,blue;
    @Inject(method="render",at=@At("TAIL")) private static void darkSky(Camera camera,float delta,ClientWorld world,int distance,float sky,CallbackInfo ci){float a=(float)(1-RitualVisuals.darkness());red*=a;green*=a;blue*=a;RenderSystem.clearColor(red,green,blue,0);}
    @Inject(method="applyFog",at=@At("TAIL")) private static void darkFog(Camera camera,BackgroundRenderer.FogType type,float distance,boolean thick,float delta,CallbackInfo ci){if(RitualVisuals.darkness()>.5){RenderSystem.setShaderFogColor(red,green,blue);RenderSystem.setShaderFogStart(24);RenderSystem.setShaderFogEnd(36);}}
}
