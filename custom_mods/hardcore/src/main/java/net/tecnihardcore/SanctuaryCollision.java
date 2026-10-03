package net.tecnihardcore;

import net.minecraft.block.*;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.*;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.*;
import net.minecraft.world.*;
import net.minecraft.entity.player.PlayerEntity;

/** Individual cells let Minecraft query the entire pedestal and its four supports. */
public final class SanctuaryCollision extends Block {
    public static final IntProperty X=IntProperty.of("x",0,6), Y=IntProperty.of("y",0,5), Z=IntProperty.of("z",0,6);
    public SanctuaryCollision(){super(Settings.create().strength(-1,3600000).nonOpaque().noBlockBreakParticles().pistonBehavior(net.minecraft.block.piston.PistonBehavior.BLOCK));setDefaultState(getStateManager().getDefaultState().with(X,3).with(Y,0).with(Z,3));}
    @Override protected void appendProperties(StateManager.Builder<Block,BlockState> b){b.add(X,Y,Z);}
    @Override public BlockRenderType getRenderType(BlockState s){return BlockRenderType.INVISIBLE;}
    public static BlockPos core(BlockState s,BlockPos p){return p.add(3-s.get(X),-s.get(Y),3-s.get(Z));}
    public static VoxelShape shape(int x,int y,int z){
        VoxelShape out=VoxelShapes.empty();
        // Exact stepped pedestal bounds: 28,24,20,22 model pixels at scale 3.5.
        double[][] base={{-3.0625,0,-3.0625,3.0625,.65625,3.0625},{-2.625,.65625,-2.625,2.625,1.09375,2.625},{-2.1875,1.09375,-2.1875,2.1875,1.75,2.1875},{-2.40625,1.75,-2.40625,2.40625,1.96875,2.40625}};
        for(double[] b:base)out=VoxelShapes.union(out,cell(b,x,y,z));
        // The supports are narrower above their caps; floating rings remain traversable.
        for(int sx:new int[]{-1,1})for(int sz:new int[]{-1,1}){
            double cx=sx*1.7015,cz=sz*1.7015;
            out=VoxelShapes.union(out,cell(new double[]{cx-.58,1.3125,cz-.58,cx+.58,4.15625,cz+.58},x,y,z));
            out=VoxelShapes.union(out,cell(new double[]{cx-.38,4.15625,cz-.38,cx+.38,5.03125,cz+.38},x,y,z));
        }
        return out.simplify();
    }
    private static VoxelShape cell(double[] b,int x,int y,int z){
        double a=Math.max(0,b[0]+.5-x),d=Math.min(1,b[3]+.5-x),e=Math.max(0,b[1]-y),f=Math.min(1,b[4]-y),g=Math.max(0,b[2]+.5-z),h=Math.min(1,b[5]+.5-z);
        return d>a&&f>e&&h>g?VoxelShapes.cuboid(a,e,g,d,f,h):VoxelShapes.empty();
    }
    @Override public VoxelShape getOutlineShape(BlockState s,BlockView w,BlockPos p,ShapeContext c){return shape(s.get(X)-3,s.get(Y),s.get(Z)-3);}
    @Override public VoxelShape getCollisionShape(BlockState s,BlockView w,BlockPos p,ShapeContext c){return getOutlineShape(s,w,p,c);}
    @Override public ActionResult onUse(BlockState s,World w,BlockPos pos,PlayerEntity p,Hand hand,BlockHitResult hit){
        BlockPos center=core(s,pos);
        if(!w.isClient&&w.getBlockState(center).isOf(Sanctuaries.CORE)&&p instanceof net.minecraft.server.network.ServerPlayerEntity player)RitualNetwork.open(player,center);
        return ActionResult.SUCCESS;
    }
    public static void install(World w,BlockPos core){
        for(int x=-3;x<=3;x++)for(int y=0;y<=5;y++)for(int z=-3;z<=3;z++){
            if(x==0&&y==0&&z==0||shape(x,y,z).isEmpty())continue;
            BlockPos p=core.add(x,y,z);BlockState existing=w.getBlockState(p);
            if(existing.isAir()||existing.isOf(Sanctuaries.COLLISION))w.setBlockState(p,Sanctuaries.COLLISION.getDefaultState().with(X,x+3).with(Y,y).with(Z,z+3),3);
        }
    }
    public static void remove(World w,BlockPos core){
        for(BlockPos p:BlockPos.iterate(core.add(-3,0,-3),core.add(3,5,3))){BlockState s=w.getBlockState(p);if(s.isOf(Sanctuaries.COLLISION)&&core(s,p).equals(core))w.setBlockState(p,Blocks.AIR.getDefaultState(),3);}
    }
}
