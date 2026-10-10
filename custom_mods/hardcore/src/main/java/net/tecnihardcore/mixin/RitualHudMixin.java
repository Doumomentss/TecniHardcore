package net.tecnihardcore.mixin;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.gui.DrawContext;
import net.tecnihardcore.RitualCinema;
import net.tecnihardcore.IntroCinema;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=InGameHud.class,priority=1100)
public abstract class RitualHudMixin {
    @Inject(method="render",at=@At("HEAD"),cancellable=true) private void cinematic(DrawContext draw,float delta,CallbackInfo ci){if(IntroCinema.active()){IntroCinema.hud(draw,delta);ci.cancel();}else if(RitualCinema.current()!=null){RitualCinema.hud(draw,delta);ci.cancel();}}
}
