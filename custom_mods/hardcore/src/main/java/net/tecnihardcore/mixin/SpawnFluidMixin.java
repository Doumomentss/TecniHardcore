package net.tecnihardcore.mixin;
import net.tecnihardcore.SpawnProtection;
import net.minecraft.world.*;
import net.minecraft.block.BlockState;
import net.minecraft.fluid.*;
import net.minecraft.util.math.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(FlowableFluid.class)
public class SpawnFluidMixin {
    @Inject(method="flow",at=@At("HEAD"),cancellable=true)
    private void protect(WorldAccess w,BlockPos p,BlockState s,Direction d,FluidState fluid,CallbackInfo ci){if(w instanceof World world&&SpawnProtection.inside(world,p))ci.cancel();}
}
