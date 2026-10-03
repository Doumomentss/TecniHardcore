package net.tecnihardcore;

import net.minecraft.block.*;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;

/** Bounded server work; visuals never imply block deletion when destruction is zero. */
final class StormDestruction {
    static void tick(Cataclysms.Hazard h){
        if(h.destruction==0||h.stopping||h.age<200||h.type>1)return;
        if(h.age%20==0)h.destroyed=0;
        int budget=WeatherRules.blockBudget(h.type,h.destruction);
        if(h.destroyed>=budget)return;
        // Earthquakes fracture during the actual pulse, not during its warning.
        if(h.type==1&&((h.age-200)%160<40||(h.age-200)%160>100))return;
        long deadline=System.nanoTime()+700_000;
        var center=h.position();var random=h.world.random;
        for(int i=0;i<12&&h.destroyed<budget&&System.nanoTime()<deadline;i++){
            double radius=h.type==0?Math.min(h.radius,h.width*.22):h.radius*.85;
            double angle=random.nextDouble()*Math.PI*2,distance=Math.sqrt(random.nextDouble())*radius;
            int x=(int)Math.floor(center.x+Math.cos(angle)*distance),z=(int)Math.floor(center.z+Math.sin(angle)*distance);
            var probe=new BlockPos(x,h.center.getY(),z);
            if(!h.world.isChunkLoaded(probe)||SpawnProtection.inside(h.world,probe))continue;
            int y=h.world.getTopY(Heightmap.Type.MOTION_BLOCKING,x,z)-1-random.nextInt(Math.max(1,WeatherRules.depth(h.type,h.destruction)));
            var pos=new BlockPos(x,y,z);var state=h.world.getBlockState(pos);var block=state.getBlock();
            if(state.isAir()||!state.getFluidState().isEmpty()||state.hasBlockEntity()||state.getHardness(h.world,pos)<0||state.getHardness(h.world,pos)>5||block==Blocks.OBSIDIAN||block==Blocks.CRYING_OBSIDIAN||block==Blocks.NETHERITE_BLOCK||block==Sanctuaries.CORE||state.isIn(BlockTags.PORTALS))continue;
            boolean soft=state.isIn(BlockTags.LEAVES)||state.isIn(BlockTags.LOGS)||state.isIn(BlockTags.DIRT)||state.isIn(BlockTags.SAND)||block==Blocks.GRAVEL||block==Blocks.SNOW||block==Blocks.SNOW_BLOCK;
            if(h.destruction<=2&&!soft)continue;
            if(h.world.breakBlock(pos,false)){h.destroyed++;}
        }
    }
}
