package net.tecnihardcore.mixin;
import net.tecnihardcore.SpawnProtection;
import net.minecraft.world.*;
import net.minecraft.block.*;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(FireBlock.class)
public class SpawnFireMixin {
    @Inject(method="trySpreadingFire",at=@At("HEAD"),cancellable=true)
    private void preventBurn(World w,BlockPos p,int spread,net.minecraft.util.math.random.Random random,int age,CallbackInfo ci){if(SpawnProtection.inside(w,p))ci.cancel();}
    @Inject(method="canPlaceAt",at=@At("HEAD"),cancellable=true)
    private void protect(BlockState s,WorldView w,BlockPos p,CallbackInfoReturnable<Boolean> ci){if(w instanceof World world&&SpawnProtection.inside(world,p))ci.setReturnValue(false);}
}
