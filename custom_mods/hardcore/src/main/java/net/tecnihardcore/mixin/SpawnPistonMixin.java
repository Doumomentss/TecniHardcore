package net.tecnihardcore.mixin;
import net.tecnihardcore.SpawnProtection;
import net.minecraft.world.World;
import net.minecraft.block.piston.PistonHandler;
import net.minecraft.util.math.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.List;
@Mixin(PistonHandler.class)
public class SpawnPistonMixin {
    @Shadow @Final private World world;
    @Shadow @Final private List<BlockPos> movedBlocks;
    @Shadow @Final private List<BlockPos> brokenBlocks;
    @Shadow @Final private Direction motionDirection;
    @Inject(method="calculatePush",at=@At("RETURN"),cancellable=true)
    private void protect(CallbackInfoReturnable<Boolean> cir){
        if(cir.getReturnValue()&&(movedBlocks.stream().anyMatch(p->SpawnProtection.inside(world,p)||SpawnProtection.inside(world,p.offset(motionDirection)))||brokenBlocks.stream().anyMatch(p->SpawnProtection.inside(world,p))))cir.setReturnValue(false);
    }
}
