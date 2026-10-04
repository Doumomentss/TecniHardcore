package net.tecnihardcore;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.world.Heightmap;

/** Called at end of a server tick and only for existing, loaded columns. */
final class StormTerrain {
    static double floor(ServerWorld world,int x,int z,double reference){
        if(!world.isChunkLoaded(new BlockPos(x,world.getBottomY(),z)))return reference;
        int highest=world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,x,z);
        var pos=new BlockPos.Mutable(x,0,z);
        return WeatherRules.floor(world.getBottomY(),world.getTopY(),highest,reference,y->{
            pos.setY(y);var state=world.getBlockState(pos);
            return !state.isIn(BlockTags.LEAVES)&&(!state.getFluidState().isEmpty()||!state.getCollisionShape(world,pos).isEmpty());
        });
    }
    static double footprint(ServerWorld world,double x,double z,double reference,int width){
        int px=(int)Math.floor(x),pz=(int)Math.floor(z),offset=Math.min(6,Math.max(2,width/30));
        double floor=floor(world,px,pz,reference);
        for(int[] delta:new int[][]{{offset,0},{-offset,0},{0,offset},{0,-offset}}){
            int sx=px+delta[0],sz=pz+delta[1];
            if(world.isChunkLoaded(new BlockPos(sx,world.getBottomY(),sz)))floor=Math.min(floor,floor(world,sx,sz,reference));
        }
        return floor;
    }
    private StormTerrain(){}
}
