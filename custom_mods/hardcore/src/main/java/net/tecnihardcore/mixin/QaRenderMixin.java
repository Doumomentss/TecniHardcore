package net.tecnihardcore.mixin;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.MinecraftClient;
import net.tecnihardcore.QaCapture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(GameRenderer.class)
public class QaRenderMixin {
 @Inject(method="render",at=@At("RETURN"))
 private void screenshot(float delta,long time,boolean tick,CallbackInfo ci){QaCapture.frame(MinecraftClient.getInstance());}
}
