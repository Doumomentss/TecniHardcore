package net.tecnihardcore;

import net.minecraft.block.*;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;

/** Connected, exposed damage, processed under a per-tick time and block budget. */
final class StormDestruction {
    static void tick(Cataclysms.Hazard h){
        if(h.destruction==0||h.stopping||h.age<200||(h.type!=0&&h.type!=1&&h.type!=4))return;
        if(h.age%20==0)h.destroyed=0;
        int pulse=(h.age-200)%160;
        if(h.type==1&&pulse==40){h.fractureCursor=0;h.pendingBlocks.clear();}
        if(h.type==1&&(pulse<40||pulse>100))return;
        int budget=h.type==0?WeatherRules.tornadoBudget(h.destruction,h.radius,h.width):WeatherRules.blockBudget(h.type,h.destruction);
        int attempts=h.type==0?Math.min(2000,Math.max(24,(budget+19)/20+8)):24;
        long deadline=System.nanoTime()+(h.type==0?3_000_000:1_200_000);
        for(int i=0;i<attempts&&h.destroyed<budget&&System.nanoTime()<deadline;i++){
            if(h.pendingBlocks.isEmpty()){
                if(h.type==4)break;
                if(h.type==1)fracture(h);else erosion(h);
            }
            BlockPos pos=h.pendingBlocks.pollFirst();if(pos==null)continue;
            if(!h.world.isChunkLoaded(pos)||SpawnProtection.inside(h.world,pos))continue;
            var state=h.world.getBlockState(pos);if(!allowed(h,pos,state))continue;
            // No breakBlock world event per block: the sampled rubble channel
            // replaces thousands of particles/sound packets with one bounded batch.
            if(h.type==0){
                var entity=h.world.getBlockEntity(pos);
                if(entity instanceof net.minecraft.inventory.Inventory inventory)inventory.clear();
                if(h.world.removeBlock(pos,false)){h.destroyed++;StormRubble.removed(h,pos,state);}
            }else if(h.world.breakBlock(pos,false))h.destroyed++;
        }
        if(h.type==0&&h.age%5==0)StormRubble.flush(h);
    }
    private static boolean allowed(Cataclysms.Hazard h,BlockPos pos,BlockState state){
        var block=state.getBlock();
        if(state.isAir()||block instanceof FluidBlock||state.getHardness(h.world,pos)<0||block==Sanctuaries.CORE||state.isIn(BlockTags.PORTALS))return false;
        if(!WeatherRules.uprootsHeavyBlocks(h.type,h.destruction)&&(state.hasBlockEntity()||state.getHardness(h.world,pos)>5||block==Blocks.OBSIDIAN||block==Blocks.CRYING_OBSIDIAN||block==Blocks.NETHERITE_BLOCK))return false;
        return h.destruction>=3||h.type==4||state.isIn(BlockTags.LEAVES)||state.isIn(BlockTags.LOGS)||state.isIn(BlockTags.DIRT)||state.isIn(BlockTags.SAND)||block==Blocks.GRAVEL||block==Blocks.SNOW||block==Blocks.SNOW_BLOCK;
    }
    private static void column(Cataclysms.Hazard h,int x,int z,int depth){
        var probe=new BlockPos(x,h.world.getBottomY(),z);
        if(!h.world.isChunkLoaded(probe)||SpawnProtection.inside(h.world,probe)||!h.world.getWorldBorder().contains(probe))return;
        int y=h.world.getTopY(Heightmap.Type.MOTION_BLOCKING,x,z)-1;
        for(int d=0;d<depth&&y-d>=h.world.getBottomY();d++){
            var pos=new BlockPos(x,y-d,z);
            if(h.type!=0||allowed(h,pos,h.world.getBlockState(pos)))h.pendingBlocks.addLast(pos);
        }
    }
    private static void erosion(Cataclysms.Hazard h){
        var center=h.position();var random=h.world.random;
        double radius=WeatherRules.tornadoDamageRadius(h.radius,h.width);int bestX=0,bestZ=0,bestY=h.world.getBottomY()-1;
        // Prefer the tallest exposed column among bounded samples: strip roofs,
        // towers and their supports before spending the whole budget in air.
        for(int i=0;i<(h.destruction>4?4:1);i++){
            double a=random.nextDouble()*Math.PI*2,d=Math.sqrt(random.nextDouble())*radius;
            int x=(int)Math.floor(center.x+Math.cos(a)*d),z=(int)Math.floor(center.z+Math.sin(a)*d);
            var probe=new BlockPos(x,h.world.getBottomY(),z);
            if(!h.world.isChunkLoaded(probe)||!h.world.getWorldBorder().contains(probe)||SpawnProtection.inside(h.world,probe))continue;
            int y=h.world.getTopY(Heightmap.Type.MOTION_BLOCKING,x,z)-1;
            if(y>bestY){bestY=y;bestX=x;bestZ=z;}
        }
        if(bestY>=h.world.getBottomY())column(h,bestX,bestZ,WeatherRules.depth(h.type,h.destruction));
    }
    private static void fracture(Cataclysms.Hazard h){
        int width=WeatherRules.fractureWidth(h.destruction),length=Math.max(8,Math.min(72,(int)(h.radius*.75))),cursor=h.fractureCursor++;
        int arm=cursor/(width*length)%6,step=cursor/width%length,offset=cursor%width-width/2;
        double a=arm*Math.PI/3+((h.age-200)/160)*.73,d=3+step;
        int x=h.center.getX()+(int)Math.round(Math.cos(a)*d-Math.sin(a)*offset),z=h.center.getZ()+(int)Math.round(Math.sin(a)*d+Math.cos(a)*offset);
        column(h,x,z,WeatherRules.depth(h.type,h.destruction));
    }
    static void impact(Cataclysms.Hazard h,Vec3d mark){
        if(h.destruction==0)return;
        int radius=WeatherRules.craterRadius(h.destruction),depth=WeatherRules.depth(4,h.destruction);
        var queued=new java.util.HashSet<>(h.pendingBlocks);
        for(int x=-radius;x<=radius;x++)for(int z=-radius;z<=radius;z++){
            double f=1-(x*x+z*z)/(double)(radius*radius);if(f<=0)continue;
            int px=(int)Math.floor(mark.x)+x,pz=(int)Math.floor(mark.z)+z;var probe=new BlockPos(px,h.world.getBottomY(),pz);
            if(!h.world.isChunkLoaded(probe)||SpawnProtection.inside(h.world,probe)||!h.world.getWorldBorder().contains(probe))continue;
            int y=h.world.getTopY(Heightmap.Type.MOTION_BLOCKING,px,pz)-1,cut=Math.max(1,(int)Math.ceil(depth*f));
            for(int i=0;i<cut&&y-i>=h.world.getBottomY()&&h.pendingBlocks.size()<4096;i++){
                var pos=new BlockPos(px,y-i,pz);if(queued.add(pos))h.pendingBlocks.addLast(pos);
            }
        }
    }
    private StormDestruction(){}
}
