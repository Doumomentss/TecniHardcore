package net.tecnihardcore.mixin;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.client.input.Input;
import net.tecnihardcore.RitualCinema;
import net.tecnihardcore.IntroCinema;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(KeyboardInput.class)
public abstract class RitualInputMixin extends Input {
    @Inject(method="tick",at=@At("TAIL")) private void cinematic(boolean slow,float factor,CallbackInfo ci){if(IntroCinema.active()||RitualCinema.current()!=null || (net.minecraft.client.MinecraftClient.getInstance().player!=null && net.minecraft.client.MinecraftClient.getInstance().player.hasStatusEffect(net.tecnihardcore.BossRoots.ROOT))){movementForward=0;movementSideways=0;jumping=false;sneaking=false;}}
}
