package net.tecnihardcore.mixin;
import net.tecnihardcore.RitualVisuals;
import me.jellysquid.mods.sodium.client.gl.shader.uniform.*;
import com.mojang.blaze3d.systems.RenderSystem;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Sodium's terrain program does not use vanilla ColorModulator; isolate its fog. */
@Pseudo
@Mixin(targets="me.jellysquid.mods.sodium.client.render.chunk.shader.ChunkShaderFogComponent$Smooth",remap=false)
public abstract class RitualSodiumFogMixin {
    @Shadow @Final private GlUniformFloat4v uFogColor;
    @Shadow @Final private GlUniformFloat uFogStart,uFogEnd;
    @Inject(method="setup",at=@At("TAIL"),remap=false) private void dome(CallbackInfo ci){
        float darkness=(float)Math.max(RitualVisuals.darkness(),net.tecnihardcore.CataclysmVisuals.darkness()*.55F);if(darkness<=0)return;float mix=darkness/.96F;float[] color=RenderSystem.getShaderFogColor();
        uFogColor.set(new float[]{color[0]*(1-darkness),color[1]*(1-darkness),color[2]*(1-darkness),color[3]});
        uFogStart.setFloat(RenderSystem.getShaderFogStart()*(1-mix));
        uFogEnd.setFloat(RenderSystem.getShaderFogEnd()*(1-mix)+1.2F*mix);
    }
}
