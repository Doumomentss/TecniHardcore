package net.tecnihardcore;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public final class SanctuaryEntity extends BlockEntity implements GeoBlockEntity {
    private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
    public SanctuaryEntity(BlockPos pos,BlockState state) {super(Sanctuaries.ENTITY,pos,state);}
    @Override public void setWorld(net.minecraft.world.World world){
        super.setWorld(world);
        // A block entity can receive its world while its chunk is still being promoted.
        // Querying that chunk here would wait for the promotion currently invoking us.
        if(world instanceof net.minecraft.server.world.ServerWorld serverWorld)serverWorld.getServer().execute(()->Sanctuaries.remember(serverWorld,pos));
    }
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this,"core",5,s-> {
            s.setAndContinue(RawAnimation.begin().thenLoop("animation.sanctuary.idle"));return PlayState.CONTINUE;
        }));
    }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() {return cache;}
}
