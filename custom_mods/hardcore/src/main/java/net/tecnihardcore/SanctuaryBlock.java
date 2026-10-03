package net.tecnihardcore;

import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.shape.*;
import net.minecraft.world.*;
import net.minecraft.block.piston.PistonBehavior;
import org.jetbrains.annotations.Nullable;

public final class SanctuaryBlock extends BlockWithEntity {
    public SanctuaryBlock() { super(AbstractBlock.Settings.create().strength(-1,3600000).nonOpaque().luminance(s->15).pistonBehavior(PistonBehavior.BLOCK)); }
    @Override public BlockEntity createBlockEntity(BlockPos pos,BlockState state) { return new SanctuaryEntity(pos,state); }
    @Override public BlockRenderType getRenderType(BlockState state) { return BlockRenderType.INVISIBLE; }
    @Override public VoxelShape getOutlineShape(BlockState s,BlockView w,BlockPos p,ShapeContext c) { return SanctuaryCollision.shape(0,0,0); }
    @Override public VoxelShape getCollisionShape(BlockState s,BlockView w,BlockPos p,ShapeContext c) { return SanctuaryCollision.shape(0,0,0); }
    @Override public ActionResult onUse(BlockState s,World w,BlockPos pos,PlayerEntity player,Hand hand,BlockHitResult hit) {
        if(!w.isClient && player instanceof net.minecraft.server.network.ServerPlayerEntity p)RitualNetwork.open(p,pos);
        return ActionResult.SUCCESS;
    }
    @Override public void onStateReplaced(BlockState state,World world,BlockPos pos,BlockState next,boolean moved) {
        if(!world.isClient && !next.isOf(this)) {Rituals.cancelAt(world.getRegistryKey(),pos);Sanctuaries.forget(world,pos);SanctuaryCollision.remove(world,pos);}
        super.onStateReplaced(state,world,pos,next,moved);
    }
}
