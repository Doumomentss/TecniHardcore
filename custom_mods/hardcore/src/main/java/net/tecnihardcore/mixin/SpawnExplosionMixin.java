package net.tecnihardcore.mixin;
import net.tecnihardcore.SpawnProtection;
import net.minecraft.world.*;
import net.minecraft.world.explosion.Explosion;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
@Mixin(Explosion.class)
public class SpawnExplosionMixin {
    @Shadow @Final private World world;
    @Shadow @Final private ObjectArrayList<BlockPos> affectedBlocks;
    @Inject(method="collectBlocksAndDamageEntities",at=@At("RETURN"))
    private void protect(CallbackInfo ci){affectedBlocks.removeIf(p->SpawnProtection.inside(world,p));}
}
